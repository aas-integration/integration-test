## Randoop and Daikon Automation

Generate unit tests for a given project and monitor their execution with Daikon to find likely invariants.

Usage:

    gradle jar
    gradle test

To run a quick test. The output should show test generation and likely invariants for the methods in src/test/resources/test01/TestClass01.java.

After building the jar, the project can be applied to an arbitrary Java project by going to the project directory and executing:

    $randoop_daikon_dir/dljc -t runner -- [build command]

More documentation and functionality will follow shortly.