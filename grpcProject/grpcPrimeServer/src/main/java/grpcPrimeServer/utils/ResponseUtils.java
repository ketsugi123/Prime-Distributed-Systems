package grpcPrimeServer.utils;

import primeServerCommunicationService.PrimeMessageState;

import javax.annotation.Nullable;

public class ResponseUtils {



    public static String stateToString(PrimeMessageState state) {
        return state == PrimeMessageState.PRIME ? "true" : "false";
    }

    public static boolean stateToBoolean(PrimeMessageState state) {
       return state == PrimeMessageState.PRIME;
    }

    public static PrimeMessageState stringToState(String state) {
        return state.equals("true") ? PrimeMessageState.PRIME : PrimeMessageState.NOT_PRIME;
    }

    @Nullable
    public static Boolean containerResponseToBool(String response) {
        switch (response) {
            case "IS_PRIME":
                return true;

            case "NOT_PRIME":
                return false;

            default:
                return null;
        }
    }
}
