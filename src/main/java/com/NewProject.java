package com;

public class NewProject {

    public static void main(String[] args){
        String a1 = null , a2 = "null", a3 = "100";
        Integer a4 = 100, a5 = 0;
        try {
            if (a1.equals(a2)) {
                a5+=1;
            }
            if(a3.equals(a4 + "")){
                a5+=2;
            }
        }catch (Exception e){
            a5+=3;
        }finally {
            a5+=4;
        }
        System.out.println(a5);
    }
}
