package grpcPrimeCalculation;

import redis.clients.jedis.Jedis;


public class GrpcPrimeCalculation {

    private static final String RESPONSE_PRIME = "IS_PRIME";
    private static final String RESPONSE_NOT_PRIME = "NOT_PRIME";
    private static final String RESPONSE_NEXT_SERVER = "NEXT_SERVER";


    public static void main(String[] args) {
        if (!isNumeric(args[0]) || !isNumeric(args[2])) {
            throw new IllegalArgumentException("Number must be provided");
        }
        Long number = Long.parseLong(args[0]);
        String message = isPrime(number) ? RESPONSE_PRIME : RESPONSE_NOT_PRIME;
        System.out.println(message);
    }

    static boolean isPrime(Long number) {

        if (number <= 1)
            return false;


        for (int i = 2; i < number; i++)
            if (number % i == 0)
                return false;

        return true;
    }


    private static boolean isNumeric(String numberArg) {
        return numberArg.chars().allMatch(Character::isDigit);
    }

}
