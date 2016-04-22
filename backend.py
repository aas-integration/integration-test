import sys, os

WORKING_DIR = os.path.dirname(os.path.realpath(__file__))



def add_project_to_corpus(project_dir):
  """ Assumes that the project_dir contains a 
  text file named TODO.TODO that contains the build command(s) for the 
  project in this directory. 
  """ 

  """ Run dljc """ #All Tim's stuff
  print "TODO"

  """ Run Randoop to generate test sources """
  print "TODO"

  """ Compile test sources """
  print "TODO"

  """ Run daikon.Chicory on tests to create dtrace file"""
  print "TODO"

  """ Precompute graph kernels that are independent of ontology stuff"""
  print "TODO" # Wenchao.

  """ run petablox """
  print "TODO"


def update_corpus_project(project_dir):
  """ This is triggered when new type annotations are being added either by 
  checker_framework or when the frontend has discovered a set of methods that
  establish certain invariants."""

  """ Recompute dot files """  #Martin S
  print "TODO"

  """ Recompute various kernels """  #Wenchao
  print "TODO"


  """ Update petablox """
  print "TODO"


def find_methods_with_property(property):
  """ TODO we need to have sth to find all methods that take a sequence as input
  and return a sequence. Should be done by LB but can be hardcoded by Tim or Martin."""
  pass


def get_dtrace_file_for_project(project):
  if project == "TODO":
    return os.path.join(WORKING_DIR, "inv_check/test.dtrace.gz")
  return "TODO"


if __name__ == '__main__':
  main()