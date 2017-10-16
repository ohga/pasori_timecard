package jp.ttlv.t.felica;

public class Polling {
    static {
        System.loadLibrary("felica_polling");
    }
    public native String do_service();
    /*
    public static void main(String[] args){
        polling pp = new polling();
        String rtn = pp.do_service();
        if(rtn != null){
            System.out.println("IDm: ["+rtn+"]\n");
        }
    }
    */
}
