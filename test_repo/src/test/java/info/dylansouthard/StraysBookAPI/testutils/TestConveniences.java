package info.dylansouthard.StraysBookAPI.testutils;

public class TestConveniences {

   public static int getTotalPages(int totalItems, int pageSize) {
       return (totalItems + pageSize - 1) / pageSize;
   }
}
