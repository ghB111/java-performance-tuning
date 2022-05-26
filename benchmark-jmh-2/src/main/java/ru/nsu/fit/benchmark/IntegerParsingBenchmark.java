package ru.nsu.fit.benchmark;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Mode;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class IntegerParsingBenchmark {

    private static final String parsedStringInteger = "212036548323188";
    private static final String parsedStringNonInteger = "1000_Nan_1235";
    private static final Pattern bakedPattern = Pattern.compile("^\\d+$");

    @Benchmark
    @BenchmarkMode(Mode.AverageTime)
    public void parseIntSuccess() {
        boolean result;
        try {
            Integer.parseInt(parsedStringInteger);
            result = true;
        } catch (NumberFormatException ignored) {
            result = false;
        }
        assert result;
    }

    @Benchmark
    @BenchmarkMode(Mode.AverageTime)
    public void parseIntFailure() {
        boolean result;
        try {
            Integer.parseInt(parsedStringNonInteger);
            result = true;
        } catch (NumberFormatException ignored) {
            result = false;
        }
        assert !result;
    }

    @Benchmark
    @BenchmarkMode(Mode.AverageTime)
    public void isDigitSuccess() {
        boolean result = true;
        for (int i = 0; i < parsedStringInteger.length(); i++) {
            if (!Character.isDigit(parsedStringInteger.charAt(i))) {
                result = false;
                break;
            }
        }
        assert result;
    }

    @Benchmark
    @BenchmarkMode(Mode.AverageTime)
    public void isDigitFailure() {
        boolean result = true;
        for (int i = 0; i < parsedStringNonInteger.length(); i++) {
            if (!Character.isDigit(parsedStringNonInteger.charAt(i))) {
                result = false;
                break;
            }
        }
        assert !result;
    }

    @Benchmark
    @BenchmarkMode(Mode.AverageTime)
    public void regexSuccess() {
        final Pattern pattern = Pattern.compile("^\\d+$");
        final Matcher matcher = pattern.matcher(parsedStringInteger);
        boolean result = matcher.find();
        assert result;
    }

    @Benchmark
    @BenchmarkMode(Mode.AverageTime)
    public void regexFailure() {
        final Pattern pattern = Pattern.compile("^\\d+$");
        final Matcher matcher = pattern.matcher(parsedStringNonInteger);
        boolean result = matcher.find();
        assert !result;
    }

    @Benchmark
    @BenchmarkMode(Mode.AverageTime)
    public void bakedRegexSuccess() {
        final Matcher matcher = bakedPattern.matcher(parsedStringInteger);
        boolean result = matcher.find();
        assert result;
    }

    @Benchmark
    @BenchmarkMode(Mode.AverageTime)
    public void bakedRegexFailure() {
        final Matcher matcher = bakedPattern.matcher(parsedStringNonInteger);
        boolean result = matcher.find();
        assert !result;
    }
}
