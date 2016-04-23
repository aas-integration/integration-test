
import sys, os
import subprocess
import traceback
import urllib
import zipfile

sys.path.insert(0, os.path.abspath('..'))
import ontology_to_daikon

WORKING_DIR = os.path.dirname(os.path.realpath(__file__))
daikon_jar = os.path.join(WORKING_DIR, "daikon.jar")  
DAIKON_SPLITTER = "====================="

def find_or_download_tools(): 
  if not os.path.isfile(daikon_jar):    
    print("Downloading annotation daikon to %s" % daikon_jar)
    urllib.urlretrieve("http://plse.cs.washington.edu/daikon/download/daikon.jar", daikon_jar)


def run_command(cmd):
  print (" ".join(cmd))
  try:
    return subprocess.check_output(cmd, stderr=subprocess.STDOUT)
  except:
    raise Exception('calling {cmd} failed\n{trace}'.format(cmd=' '.join(cmd),trace=traceback.format_exc()))


def run_daikon_on_dtrace_file(dtrace_file, classpath=daikon_jar, checked_invariant=None):

  cmd = ["java", "-classpath", classpath, "daikon.Daikon", dtrace_file]
  if checked_invariant:
    print ("DO SOMETHING")
    cmd += ["--disable-all-invariants", "--user-defined-invariant", checked_invariant]

  return run_command(cmd)



def find_ppts_that_establish_inv_in_daikon_output(daikon_output, inv_substring):
  ppts_with_inv = []
  start_of_new_block = False
  current_method = None
  lines = daikon_output.splitlines(True)
  i = 0
  while (i<len(lines)):
    if DAIKON_SPLITTER in lines[i]:
      i+=1
      ppt_name = lines[i]
      i+=1
      while (i<len(lines) and DAIKON_SPLITTER not in lines[i]):
        if inv_substring in lines[i]:
          ppts_with_inv+=[ppt_name]
        i+=1
    else:
      i+=1
  return ppts_with_inv


def find_ppts_that_establish_inv(dtrace_file, pattern_class_dir, pattern_class_name):
  """
  This is the main method to be called from the outside.
  INPUT: dtrace_file - for a given project
         pattern_class_name - the root class dir for the daikon pattern that needs to be added to the CP when running daikon
         pattern_class_name - qualified name of the pattern class
  OUTPUT: set of daikon program points (ppts) that establish the given invariant. 
  """
  daikon_output = run_daikon_on_dtrace_file(dtrace_file, daikon_jar+":"+pattern_class_dir, pattern_class_name)
  ppts = find_ppts_that_establish_inv_in_daikon_output(daikon_output, pattern_class_name)
  return ppts


def test(): 
  test_dtrace = os.path.join(WORKING_DIR, "test.dtrace.gz")
  test_inv_name = "TestInvariant"
  ontology_to_daikon.create_daikon_invariant(os.path.join(WORKING_DIR, "README.md"), test_inv_name)
  cmd = ["javac", "-classpath", "daikon.jar:.", test_inv_name+".java"]
  run_command(cmd)  
  print ("Finding program points")
  ppts = find_ppts_that_establish_inv(test_dtrace, WORKING_DIR, test_inv_name)
  print ("deleting temp files")
  os.remove(test_inv_name+".class")
  os.remove(test_inv_name+".java")
  os.remove("test.inv.gz")
  #output = run_daikon_on_dtrace_file(test_dtrace, checked_invariant="daikon.inv.unary.sequence.EltwiseIntLessThan")
  #print output
  #ppts = find_ppts_that_establish_inv_in_daikon_output(output, " sorted by ")
  print ("Methods that establish FirstMuseInvariant:")
  for ppt in ppts:
    print ppt


def main(): 
  find_or_download_tools()
  test()


if __name__ == '__main__':
  main()