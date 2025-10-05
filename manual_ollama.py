from codebase_whisperer.llm.ollama import OllamaClient

def main():
    client = OllamaClient("http://localhost:11434")

    # 1. Test embeddings
    print("=== Testing /api/embeddings ===")
    vecs = client.embed("nomic-embed-text", ["hello world"])
    print(f"Embedding length: {len(vecs[0])}")
    print(f"First 5 numbers: {vecs[0][:5]}")

    # 2. Test chat
    print("\n=== Testing /api/chat ===")
    resp = client.chat("llama3:8b", [{"role": "user", "content": "Say hello in pirate voice."}], stream=False)
    print("Chat response:", resp)

if __name__ == "__main__":
    main()
