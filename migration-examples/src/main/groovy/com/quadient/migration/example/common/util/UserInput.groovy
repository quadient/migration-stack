package com.quadient.migration.example.common.util

interface UserInput {
    boolean confirm(String message)

    String choose(String message, List<String> options)

    String chooseOrType(String message, List<String> options, String defaultValue)

    static UserInput from(Binding binding) {
        def appInput = binding.variables["input"]
        return appInput == null ? new StdinUserInput() : appInput as UserInput
    }

    class StdinUserInput implements UserInput {
        private final BufferedReader reader
        private final PrintStream out

        private StdinUserInput() {
            this(System.in.newReader(), System.out)
        }

        private StdinUserInput(BufferedReader reader, PrintStream out) {
            this.reader = reader
            this.out = out
        }

        @Override
        boolean confirm(String message) {
            out.println("$message (y/n)")
            return readLine() == "y"
        }

        @Override
        String choose(String message, List<String> options) {
            out.println(message)
            printOptions(options)

            while (true) {
                def selected = optionAt(readLine(), options)
                if (selected != null) {
                    return selected
                }
                out.println("Invalid selection. Please enter a valid number:")
            }
        }

        @Override
        String chooseOrType(String message, List<String> options, String defaultValue) {
            out.println("$message (leave empty for: $defaultValue)")
            printOptions(options)

            def line = readLine()
            if (line.isEmpty()) {
                return defaultValue
            }

            return optionAt(line, options) ?: line
        }

        private String readLine() {
            def line = reader.readLine()
            if (line == null) {
                throw new IllegalStateException("No input available on stdin")
            }
            return line.trim()
        }

        private void printOptions(List<String> options) {
            options.eachWithIndex { option, i -> out.println("${i + 1}) $option") }
        }

        private static String optionAt(String line, List<String> options) {
            if (!line.isInteger()) {
                return null
            }
            def index = line.toInteger() - 1
            return index >= 0 && index < options.size() ? options[index] : null
        }
    }
}
