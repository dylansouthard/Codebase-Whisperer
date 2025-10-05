package info.dylansouthard.StraysBookAPI.constants;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

public class PaginationConsts {
    public static final int DEFAULT_NOTIFICATION_PAGE_SIZE = 10;
    public static final int DEFAULT_ANIMAL_NOTIFICATION_PAGE_SIZE = 10;
    public static final int HP_NOTIFICATION_PAGE_SIZE = 20;
    public static final int DEFAULT_WATCHED_ANIMAL_PAGE_SIZE = 20;

    public static final String SORT_FIELD_CREATED_AT = "createdAt";
    public static final String SORT_FIELD_DATE = "date";
    public static final String SORT_FIELD_PRIORITY = "priority";

    public static final int DEFAULT_CARE_EVENT_PAGE_SIZE = 20;

    public static Pageable notificationPageable() {
        return notificationPageable(null, null);
    }

    public static Pageable notificationPageable(Integer pageNumber, Integer pageSize) {
        int pn = pageNumber == null ? 0 : pageNumber;
        int ps = pageSize == null ? DEFAULT_NOTIFICATION_PAGE_SIZE : pageSize;
        return PageRequest.of(pn, ps,
                Sort.by(SORT_FIELD_CREATED_AT).descending());
    }


    public static Pageable careEventPageable(Integer pageNumber, Integer pageSize) {
        int pn = pageNumber == null ? 0 : pageNumber;
        int ps = pageSize == null ? DEFAULT_CARE_EVENT_PAGE_SIZE : pageSize;
        return PageRequest.of(pn, ps,
                Sort.by(SORT_FIELD_DATE).descending());
    }

    public static Pageable watchedAnimalPageable(Integer pageNumber, Integer pageSize) {
        int pn = pageNumber == null ? 0 : pageNumber;
        int ps = pageSize == null ? DEFAULT_ANIMAL_NOTIFICATION_PAGE_SIZE : pageSize;
        return PageRequest.of(pn, ps);
    }

    public static Pageable notificationForAnimalProfile() {
        return notificationForAnimalProfile(null, null);
    }

    public static Pageable notificationForAnimalProfile(Integer pageNumber, Integer pageSize) {
        int pn = pageNumber == null ? 0 : pageNumber;
        int ps = pageSize == null ? DEFAULT_ANIMAL_NOTIFICATION_PAGE_SIZE : pageSize;
        return PageRequest.of(pn, ps,
                Sort.by(SORT_FIELD_CREATED_AT).descending());
    }



}
