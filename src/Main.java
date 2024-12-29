import java.util.*;
import java.util.regex.Pattern;

public class Main {

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        System.out.println("Enter Ruby-like code (end with 'END'):");
        //reads input from consol while you do not tipe END
        StringBuilder code = new StringBuilder();
        String line;
        while (!(line = scanner.nextLine()).equals("END")) {
            code.append(line).append("\n");
        }

        executeRubyLikeCode(code.toString().trim(), new HashMap<>(), new HashMap<>());
    }

    public static void executeRubyLikeCode(String code, HashMap<String, Object> localVariables, HashMap<String, String> functions) {
        try {
            String[] lines = code.split("\n");
            for (int i = 0; i < lines.length; i++) {
                String line = lines[i].trim();

                if (line.startsWith("#") || line.isEmpty()) {
                    continue; // Ignore comments and empty lines
                }

                if (line.startsWith("def")) {
                    // Function definition with parameters
                    String header = line.substring(4, line.indexOf("(")).trim();
                    String paramList = line.substring(line.indexOf("(") + 1, line.indexOf(")")).trim();
                    List<String> params = Arrays.asList(paramList.split(","));
                    int start = i + 1;
                    StringBuilder functionBody = new StringBuilder();
                    while (!lines[start].trim().equals("end")) {
                        functionBody.append(lines[start]).append("\n");
                        start++;
                    }
                    functions.put(header, functionBody.toString().trim() + ";" + String.join(",", params));
                    i = start;
                } else if (line.startsWith("return")) {
                    // Handle return statement
                    throw new ReturnException(evaluateExpression(line.substring(6).trim(), localVariables));
                } else if (line.startsWith("if")) {
                    // Conditional handling
                    StringBuilder conditionBlock = new StringBuilder();
                    int start = i;
                    while (!lines[start].trim().equals("end")) {
                        conditionBlock.append(lines[start]).append("\n");
                        start++;
                    }
                    String[] conditionParts = conditionBlock.toString().split("\n", 2);
                    boolean condition = (boolean) evaluateExpression(conditionParts[0].substring(3).trim(), localVariables);
                    if (condition) {
                        executeRubyLikeCode(conditionParts[1].trim(), localVariables, functions);
                    }
                    i = start;
                } else if (line.startsWith("puts")) {
                    // Handle 'puts'
                    String message = line.substring(4).trim();
                    System.out.println(evaluateExpression(message, localVariables));
                } else if (line.startsWith("for")) {
                    // For loop
                    String[] parts = line.split(" ");
                    String loopVar = parts[1];
                    String[] range = parts[3].split("\\.\\.");
                    int start = Integer.parseInt(range[0]);
                    int end = Integer.parseInt(range[1]);
                    int bodyStart = i + 1;
                    StringBuilder loopBody = new StringBuilder();
                    while (!lines[bodyStart].trim().equals("end")) {
                        loopBody.append(lines[bodyStart]).append("\n");
                        bodyStart++;
                    }
                    for (int j = start; j <= end; j++) {
                        localVariables.put(loopVar, j);
                        executeRubyLikeCode(loopBody.toString().trim(), localVariables, functions);
                    }
                    i = bodyStart; // Skip to the end of the loop
                } else if (line.contains("=")) {
                    // Variable assignment
                    String[] parts = line.split("=");
                    String varName = parts[0].trim();
                    Object value = evaluateExpression(parts[1].trim(), localVariables);
                    localVariables.put(varName, value);
                } else if (line.matches("\\w+\\(.*\\)")) {
                    // Function call
                    String functionName = line.substring(0, line.indexOf("("));
                    String params = line.substring(line.indexOf("(") + 1, line.indexOf(")")).trim();
                    if (functions.containsKey(functionName)) {
                        String[] paramArray = functions.get(functionName).split(";");
                        String functionBody = paramArray[0];
                        String[] paramNames = paramArray[1].split(",");
                        HashMap<String, Object> functionLocalVariables = new HashMap<>(localVariables);
                        String[] passedParams = params.split(",");
                        for (int p = 0; p < paramNames.length; p++) {
                            functionLocalVariables.put(paramNames[p].trim(), evaluateExpression(passedParams[p].trim(), localVariables));
                        }
                        executeRubyLikeCode(functionBody.trim(), functionLocalVariables, functions);
                    } else {
                        handleSpecialOperations(functionName, params, localVariables);
                    }
                }
            }
        } catch (ReturnException re) {
            localVariables.put("return", re.value);
        } catch (Exception e) {
            System.out.println("Error executing code: " + e.getMessage());
        }
    }

    private static Object evaluateExpression(String expr, HashMap<String, Object> localVariables) {
        expr = expr.trim();
        if (expr.matches("\\d+")) {
            return Integer.parseInt(expr);
        } else if (localVariables.containsKey(expr)) {
            return localVariables.get(expr);
        } else if (expr.matches(".*[+\\-*/%].*")) {
            String[] operators = {"+", "-", "*", "/", "%"};
            for (String op : operators) {
                if (expr.contains(op)) {
                    String regexSafeOp = Pattern.quote(op);
                    String[] parts = expr.split(regexSafeOp);
                    int left = (int) evaluateExpression(parts[0].trim(), localVariables);
                    int right = (int) evaluateExpression(parts[1].trim(), localVariables);
                    return switch (op) {
                        case "+" -> left + right;
                        case "-" -> left - right;
                        case "*" -> left * right;
                        case "/" -> left / right;
                        case "%" -> left % right;
                        default -> 0;
                    };
                }
            }
        } else if (expr.equals("true") || expr.equals("false")) {
            return Boolean.parseBoolean(expr);
        }
        throw new IllegalArgumentException("Invalid expression: " + expr);
    }

    private static void handleSpecialOperations(String functionName, String params, HashMap<String, Object> localVariables) {
        switch (functionName) {
            case "is_prime" -> {
                int n = (int) evaluateExpression(params, localVariables);
                System.out.println(n + " is prime: " + isPrime(n));
            }
            case "sum_N_numbers" -> {
                int n = (int) evaluateExpression(params, localVariables);
                System.out.println("Sum of first " + n + " numbers: " + sumNNumbers(n));
            }
            case "factorial" -> {
                int n = (int) evaluateExpression(params, localVariables);
                System.out.println("Factorial of " + n + ": " + factorial(n));
            }
            case "gcd" -> {
                String[] args = params.split(",");
                int x = (int) evaluateExpression(args[0].trim(), localVariables);
                int y = (int) evaluateExpression(args[1].trim(), localVariables);
                System.out.println("GCD of " + x + " and " + y + ": " + gcd(x, y));
            }
            case "reverse" -> {
                int n = (int) evaluateExpression(params, localVariables);
                System.out.println("Reversed number: " + reverse(n));
            }
            default -> throw new IllegalArgumentException("Unknown function: " + functionName);
        }
    }

    private static boolean isPrime(int n) {
        if (n <= 1) return false;
        if (n == 2 || n == 3) return true;
        for (int i = 2; i <= Math.sqrt(n); i++) {
            if (n % i == 0) return false;
        }
        return true;
    }

    private static int sumNNumbers(int n) {
        return n * (n + 1) / 2;
    }

    private static int factorial(int n) {
        int result = 1;
        for (int i = 2; i <= n; i++) {
            result *= i;
        }
        return result;
    }

    private static int gcd(int x, int y) {
        while (y != 0) {
            int r = x % y;
            x = y;
            y = r;
        }
        return x;
    }

    private static int reverse(int n) {
        int result = 0;
        while (n != 0) {
            result = result * 10 + n % 10;
            n /= 10;
        }
        return result;
    }

    static class ReturnException extends RuntimeException {
        Object value;

        ReturnException(Object value) {
            this.value = value;
        }
    }
}
