import os, sys, shutil, fnmatch
import subprocess32 as subprocess

petablox_out_dir = "petablox_output"

file_dir = os.path.abspath(os.path.dirname(__file__))

def run_cmd(cmd, dir):
	p = subprocess.Popen(cmd,stdout=subprocess.PIPE,stderr=subprocess.PIPE, cwd=dir)
	try:
		out, err = p.communicate(timeout=1200)
	except subprocess.TimeoutExpired as err:
		print err

def main(corpus_dir):
	benchmark_dir = os.path.abspath(corpus_dir)
	
	for base_dir, between, file_name in os.walk(benchmark_dir):
		for cf in fnmatch.filter(file_name, 'build.xml'):
			print ("Note: We assume that the project has already been compiled by Randoop.")
			cmd = ['java']
			cmd.append("-cp")
			cmd.append(os.path.join(file_dir, "petablox.jar"))
			cmd.append("-Dpetablox.reflect.kind=none")
			cmd.append("-Dpetablox.run.analyses=cipa-0cfa-dlog")
			cmd.append("petablox.project.Boot")
			run_cmd(cmd, os.path.abspath(base_dir))
			

if __name__ == '__main__':
	if len(sys.argv) < 2:
		print "run with build_all.py [corpus dir]"
	else:
		main(sys.argv[1])