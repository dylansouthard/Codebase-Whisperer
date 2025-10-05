# codebase_whisperer/chunking/plain.py
import re
from typing import List, Iterable, Tuple
from .common import split_by_size

def _split_paragraph(p: str, max_chars: int) -> List[str]:
    """Hard-split a paragraph that exceeds max_chars without producing empty chunks."""
    if not p:
        return []
    if max_chars <= 0:
        return [p]
    return split_by_size(p, max_chars)

def chunk_plain(text: str, max_chars: int, min_chars: int) -> List[str]:
    """
    Paragraph-aware chunking with these guarantees:
      1) Initial packing never exceeds max_chars.
      2) Merge phase makes every non-final chunk >= min_chars when possible:
         - Borrow from next chunk without exceeding max_chars.
         - Only slice from 'sliceable' pieces (those produced by hard-splitting a long paragraph).
         - Do not slice whole (short) paragraphs; move them whole only if they fit.
      3) Last chunk may remain < min_chars (tests allow this), unless it can be absorbed into the
         previous chunk without exceeding max_chars.
      4) If min_chars > max_chars, greedily collapse adjacent chunks (allowed to exceed max_chars)
         to reach min_chars while preserving paragraph boundaries.
    """
    if not text:
        return []

    # --- Phase 0: paragraphize and mark whether a piece is a hard-slice (True) or a whole paragraph (False)
    paras = [p.strip() for p in re.split(r"\n\s*\n", text) if p.strip()]

    # pieces: List[Tuple[str, bool]] where bool == True => came from hard-splitting a long paragraph
    pieces: List[Tuple[str, bool]] = []
    for p in paras:
        if len(p) <= max_chars:
            pieces.append((p, False))  # whole paragraph
        else:
            for seg in _split_paragraph(p, max_chars):
                pieces.append((seg, True))  # sliceable segment

    if not pieces:
        return []

    # --- Phase 1: initial pack (cap by max_chars), keeping per-chunk piece boundaries/flags
    # chunk is List[Tuple[text, is_slice]]
    chunks: List[List[Tuple[str, bool]]] = []
    cur: List[Tuple[str, bool]] = []
    cur_len = 0

    def _sep_len(has_text: bool) -> int:
        return 2 if has_text else 0  # "\n\n"

    def _append_piece(buf: List[Tuple[str,bool]], buf_len: int, piece: Tuple[str,bool]) -> Tuple[bool,int]:
        """Try to append piece to buf without exceeding max_chars (accounts for separator)."""
        text, is_slice = piece
        need = len(text) + _sep_len(buf_len > 0)
        if buf_len + need <= max_chars:
            if buf_len > 0:
                buf.append(("\n\n", True))  # separator as a tiny sliceable marker (we won’t slice it anyway)
                buf_len += 2
            buf.append((text, is_slice))
            buf_len += len(text)
            return True, buf_len
        return False, buf_len

    for piece in pieces:
        ok, cur_len = _append_piece(cur, cur_len, piece)
        if not ok:
            # flush current chunk, start a new one
            if cur:
                chunks.append(cur)
            cur = []
            cur_len = 0
            # piece is guaranteed <= max (by construction), so it fits into a fresh chunk
            _, cur_len = _append_piece(cur, cur_len, piece)
    if cur:
        chunks.append(cur)

    # Helper to convert chunk pieces -> text and length
    def chunk_text_and_len(chunk: List[Tuple[str,bool]]) -> Tuple[str, int]:
        # chunk includes explicit "\n\n" separators as pieces
        s = "".join(t for (t, _flag) in chunk)
        return s, len(s)

    # --- Phase 2A: if min_chars <= max_chars, try to raise non-final chunks using borrowing WITHOUT exceeding max_chars
    if min_chars <= max_chars:
        i = 0
        while i < len(chunks) - 1:
            # compute current text/len
            cur_txt, cur_len = chunk_text_and_len(chunks[i])

            # while current chunk is too small and there is a donor
            while cur_len < min_chars and i < len(chunks) - 1:
                donor = chunks[i + 1]
                if not donor:
                    # empty donor (shouldn’t happen), drop it
                    chunks.pop(i + 1)
                    if i >= len(chunks) - 1:
                        break
                    continue

                # room left in current chunk under max (account for separator if we take anything)
                room = max_chars - cur_len - 2
                if room <= 0:
                    break

                # Donor starts with either a separator piece (from earlier packing) or a real piece.
                # Skip any leading separator in donor (we’ll add our own).
                while donor and donor[0][0] == "\n\n":
                    donor.pop(0)

                if not donor:
                    chunks.pop(i + 1)
                    if i >= len(chunks) - 1:
                        break
                    continue

                first_text, first_is_slice = donor[0]

                took_any = False
                if first_is_slice:
                    # Allowed to slice: take only what fits in 'room'
                    take = min(room, len(first_text))
                    if take > 0:
                        # append separator if current has content
                        if cur_len > 0:
                            chunks[i].append(("\n\n", True))
                            cur_len += 2
                        head = first_text[:take]
                        tail = first_text[take:]
                        chunks[i].append((head, True))
                        cur_len += len(head)
                        took_any = True
                        if tail:
                            donor[0] = (tail, True)
                        else:
                            donor.pop(0)
                else:
                    # Whole paragraph: move it only if the whole piece fits
                    need = len(first_text)
                    if need <= room:
                        if cur_len > 0:
                            chunks[i].append(("\n\n", True))
                            cur_len += 2
                        chunks[i].append((first_text, False))
                        cur_len += need
                        donor.pop(0)
                        took_any = True

                # Clean up donor if empty (also strip leading separators again)
                while donor and donor[0][0] == "\n\n":
                    donor.pop(0)
                if not donor:
                    chunks.pop(i + 1)

                if not took_any:
                    # Can’t take more without exceeding max or breaking whole-paragraph rule
                    break

            i += 1

        # Last-chunk tiny tail: try to absorb into previous only if it DOES NOT exceed max_chars
        if len(chunks) >= 2:
            last_txt, last_len = chunk_text_and_len(chunks[-1])
            if last_len < min_chars:
                prev_txt, prev_len = chunk_text_and_len(chunks[-2])
                room = max_chars - prev_len - 2
                if room > 0:
                    # move as much as fits (do not exceed max). Prefer whole-paragraph move if possible.
                    donor = chunks[-1]
                    # skip leading separators
                    while donor and donor[0][0] == "\n\n":
                        donor.pop(0)

                    moved_any = False
                    while donor and room > 0:
                        t, is_slice = donor[0]
                        need = len(t)
                        if not is_slice and need <= room:
                            # move whole paragraph
                            if chunks[-2]:
                                chunks[-2].append(("\n\n", True))
                                prev_len += 2
                            chunks[-2].append(donor.pop(0))
                            prev_len += need
                            room = max_chars - prev_len - 2
                            moved_any = True
                        elif is_slice:
                            take = min(room, need)
                            if take > 0:
                                if chunks[-2]:
                                    chunks[-2].append(("\n\n", True))
                                    prev_len += 2
                                head, tail = t[:take], t[take:]
                                chunks[-2].append((head, True))
                                prev_len += len(head)
                                room = max_chars - prev_len - 2
                                if tail:
                                    donor[0] = (tail, True)
                                else:
                                    donor.pop(0)
                                moved_any = True
                            else:
                                break
                        else:
                            # whole paragraph but doesn't fit; stop
                            break

                    # drop last chunk if fully consumed
                    if not donor:
                        chunks.pop()

    else:
        # --- Phase 2B: min_chars > max_chars → greedily collapse adjacent chunks (allowed to exceed max) until non-final chunks meet min
        i = 0
        while i < len(chunks) - 1:
            cur_txt, cur_len = chunk_text_and_len(chunks[i])
            if cur_len >= min_chars:
                i += 1
                continue
            # Merge whole next chunk (preserve paragraph boundaries)
            # ensure single separator between chunks
            if chunks[i] and chunks[i][-1][0] != "\n\n":
                chunks[i].append(("\n\n", True))
            # skip any leading separators in donor
            while chunks[i+1] and chunks[i+1][0][0] == "\n\n":
                chunks[i+1].pop(0)
            chunks[i].extend(chunks[i+1])
            chunks.pop(i + 1)
            # don’t advance i; we may need to keep merging

    # Materialize final strings
    out: List[str] = []
    for chunk in chunks:
        s, _ = chunk_text_and_len(chunk)
        if s:  # avoid empties
            out.append(s)
    return out