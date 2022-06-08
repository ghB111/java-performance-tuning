package ru.nsu.fit.cpucheker;

public class CPUInfo {
   static {
      System.loadLibrary("cpu");
   }
 
   public static native void fillInfo(Info info);

   static public class Info {
       public String cpuFamily = "undefined";
       public int coresNum = -1;
   }
 
   public static void main(String[] args) {
       Info info = new Info();
       System.out.println("Info before:");
       System.out.println(info.cpuFamily);
       System.out.println(info.coresNum);

       fillInfo(info);

       System.out.println("Info after:");
       System.out.println(info.cpuFamily);
       System.out.println(info.coresNum);
   }
}

