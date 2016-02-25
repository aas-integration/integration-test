# Java Benchmark Expressions

Because of the complex nature of nix, there is not a run.sh file in this
directory. On the other hand this will be a semi-short introduction to
how to use *JBX*. 

JBX builds on top of the nix package manager, a tool for describing
packages, which will always compile. There is a lot of reasons to use
nix, here are a few:

*   **Purity**, if a benchmark compiles once, it will again.
*   **Consistency**, nix will force us to write our analyses, and the
    benchmarks in a consistent way.
*   **Modularity**, when a analysis has been defined it will work on any of
    our growing base of benchmarks.
*   **Caching**, all part results for analyses can be shared between
    analyses without rerunning and even between researching teams.

## Setting it up

We will just go over the easiest setup, to get an advanced setup please
read the readme [here](https://github.com/ucla-pls/jbx), or contact us.

```sh
git clone --recursive "https://github.com/ucla-pls/jbx"
```

Setup vagrant on your computer: 

1. Download from [here](https://www.vagrantup.com/).

2. Install the `vagrant-nixos` plugin: 
   ```sh
   $ vagrant plugin install vagrant-nixos 
   ``` 

3. Start vagrant (this might take some time):
    ```sh
    $ vagrant up
    $ vagrant ssh
    $ cd /vagrant
    ```

Then inspect the `environment.nix.sample` file and save the right
information to `environment.nix`.

Add any proprietary files to the `proprietary/` folder. Either download
the appropriate files from the distributors or ask us for a zip. Using 
a proprietary tool without having the file will spawn an error, which 
should inform you of the filename needed. 

## Getting things done

We have tried to make *JBX* as easy to use as possible. Even though that
all features are executable through nix commands, we have created an
array of tools that will do most of the tasks. All commands are
prepended with `./jbx` and will call different scripts in the `helpers/`
directory. The most useful is:

*   `build`: Can build a benchmark, this is crucial in the development proccess.

    This example builds avrora with java 6:
    
    ```sh
    $ ./jbx build --java 6 avrora
    ```

*   `run`: Run a dynamic analysis on a benchmark with some inputs.
    This example runs the emma reachable methods analysis
   
    ```sh
    $ ./jbx run --java 6 -a reachable-methods.emma avrora-harness -- small 
    ```
    In the scope of JBX, run is also just a dynnamic analysis and can
    just be runlike:

    ```sh
    $ ./jbx run avrora-harness -- small 
    ```

*   `analyse`: Given an analysis and a benchmark, run the analysis on the
    benchmark.

    This example runs the `runAll` analysis on the avrora-harness
    benchmark:

    ```sh
    $ ./jbx analysis --java 6 run.runAll avrora-harness
    ```

*   `petablox`: This is a tool directed solely agains petablox, because of all its
    options. Some predefined analyses can be run with the `analyse` tool
    but petablox gives alot more flexablility:

    ```sh
    $ ./jbx petablox --reflect external \
        -a cipa-0cfa-dlog \
        -a cicg2dot-java \
        fop-harness
    ```

    Also we will be adding functionallity to export a petablox analysis
    to a logicblox database, running custom scripts, and droping
    directly into an interactive mode.

## Corpus Integration

Using the `do-like-javac` we have started developing a tool that is able
to convert corpus benchmarks into nix scripts, which can then be pushed 
to the *JBX* repository. 

```sh
$ ./jbx add-benchmark url
```
Will output the corresponding nix script after performing the
downloads and do-like-javac. We can then do the testing:

```sh
$ ./jbx add-benchmark --test url
```
JBX will then test the nix script with an array of analyses ensuring
that the builds and is well formed. After all tests are passes we can
commit it to the *JBX* repository:

```sh
$ ./jbx add-benchmark --save url
```

## Exporting 

Exporting a single benchmark to a logicblox server will someday be as
simple as writing.

```sh
$ ./jbx petablox --engine logicblox4 --export some@server <benchmarks>
```
