
import sys, os
import subprocess
import traceback
import urllib
import zipfile



WORKING_DIR = os.path.dirname(os.path.realpath(__file__))
annotation_tools = os.path.join(WORKING_DIR, "annotation-tools")	

def find_or_download_tools():	
	if not os.path.isdir(annotation_tools):
		annotation_tools_zip = annotation_tools+".zip"
		print("Downloading annotation tools to %s" % annotation_tools_zip)
		urllib.urlretrieve("http://types.cs.washington.edu/annotation-file-utilities/current/annotation-tools-3.6.27.zip", annotation_tools_zip)
		with zipfile.ZipFile(annotation_tools_zip, "r") as z:
			z.extractall(WORKING_DIR)
		subprocess.call(['chmod', '-R', '+x', annotation_tools])


def run_command(cmd):
	try:
		print (subprocess.check_output(cmd, stderr=subprocess.STDOUT))
	except:
		print ('calling {cmd} failed\n{trace}'.format(cmd=' '.join(cmd),trace=traceback.format_exc()))


def merge_jaif_into_class(class_file_name, jaif_file_name):
	script = os.path.join(annotation_tools, "annotation-file-utilities/scripts/insert-annotations")
	cmd = [script, os.path.abspath(class_file_name), os.path.abspath(jaif_file_name)]
	run_command(cmd)


def test():	
	test_java = os.path.join(annotation_tools, "annotation-file-utilities/tests/MethodMultiple.java")
	test_jaif = os.path.join(annotation_tools, "annotation-file-utilities/tests/MethodMultiple.jaif")
	print ("Running test with {}".format(test_jaif))	
	cmd = ["javac", "-g", test_java, "-d", WORKING_DIR]
	print ("Compiling: {}".format(" ".join(cmd)))
	run_command(cmd)
	print ("Done Compiling.")
	test_class = os.path.join(WORKING_DIR, "annotator/tests/MethodMultiple.class")
	if not os.path.isfile(test_class):
		print "Failed to compile test. Stopping."
		return
	print ("Inserting into {}".format(test_class))
	merge_jaif_into_class(test_class, test_jaif)
	print ("Done. Annotated class written to: {}".format(test_class))
	cmd = ["javap", "-v", test_class]
	run_command(cmd)

def main():	
	find_or_download_tools()
	test()


if __name__ == '__main__':
	main()