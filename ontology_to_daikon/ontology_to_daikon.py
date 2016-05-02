
import sys, os
import subprocess
import traceback
import urllib

WORKING_DIR = os.path.dirname(os.path.realpath(__file__))

def create_daikon_invariant(ontology_invariant_file, invariant_name):
  """ takes a file that contains one invariant from 
      the ontology and returns a Java file that can
      be used with Daikon to check for this invariant.

      You need to provide an invariant name for the 
      internal representation in Daikon on to easily
      identify later if your invariant holds on a method
  """

  pattern_java_file_name = "./"+invariant_name+".java"

  operator = "<="
  with open(ontology_invariant_file, 'r') as in_file:
    #TODO: do something meaningful here.
    for line in in_file.readlines():
      operator = line.strip()
      break
    pass

  with open(pattern_java_file_name, 'w') as out_file:
    with open(os.path.join(WORKING_DIR, "invariant.java.prototype"), 'r') as proto_file:
      for line in proto_file.readlines():
        if "$OPERATOR$" in line:
          line = line.replace("$OPERATOR$", operator)
        out_file.write(line.replace("$INVARIANT_NAME$", invariant_name))

  return pattern_java_file_name


def test(): 
  create_daikon_invariant("README.md", "MyTestInvariant")
  pass


def main():   
  test()


if __name__ == '__main__':
  main()