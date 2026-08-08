package com.apitester.generator.common;

public class ValueUtils {

    public static String CemalToWords(String val){
        return val.replaceAll("(?<!^)(?=[A-Z])", " ")
                .substring(0, 1).toUpperCase() + val.replaceAll("(?<!^)(?=[A-Z])", " ").substring(1);
    }
}
