import os, sys, shutil, fnmatch
import subprocess32 as subprocess

#-cp ../petablox.jar -Dpetablox.reflect.kind=dynamic -Dpetablox.run.analyses=cipa-0cfa-dlog petablox.project.Boot
def run_cmd(cmd, dir):
	p = subprocess.Popen(cmd,stdout=subprocess.PIPE,stderr=subprocess.PIPE, cwd=dir)
	try:
		out, err = p.communicate(timeout=1200)
	except subprocess.TimeoutExpired as err:
		print err


def setup_randoop(dir):
	print ("Build Randoop scripts for {}".format(dir))

	dljc_path = os.path.join(os.path.abspath(os.path.dirname(__file__)), "dljc/bin/do-like-javac.py")

	cmd = list()
	cmd.append(dljc_path)
	cmd.append("-t")
	cmd.append("randoop")
	cmd.append("--")
	cmd.append("ant")
	cmd.append("clean")
	cmd.append("compile")

	print cmd

	run_cmd(cmd, dir)


def setup_randoop_scripts(benchmark_dir):
	for base_dir, between, file_name in os.walk(benchmark_dir):
		for cf in fnmatch.filter(file_name, 'build.xml'):
			setup_randoop(os.path.abspath(base_dir))

def run_randoop(benchmark_dir):
	for base_dir, between, file_name in os.walk(benchmark_dir):
		for cf in fnmatch.filter(file_name, 'run_randoop_*'):
			working_dir = os.path.abspath(base_dir)
			print ("Running Randoop for {}".format(working_dir))
			run_cmd(["chmod", "a+x", os.path.join(working_dir, cf)],os.path.abspath(base_dir))
			run_cmd(["./{}".format(cf)],os.path.abspath(base_dir))


def main():	
	current_dir = os.path.dirname(os.path.abspath(__file__))
	benchmark_dir = os.path.abspath(os.path.join(current_dir, "../corpus/benchmarks"))
	print("Generating Randoop scripts.")
	setup_randoop_scripts(benchmark_dir)
	print("Executing Randoop scripts.")
	run_randoop(benchmark_dir)


if __name__ == '__main__':
	main()
