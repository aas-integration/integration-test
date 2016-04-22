import os, subprocess, traceback, sys
from contextlib import contextmanager

def run_cmd(cmd, print_output=False):
  print ("Running %s" % cmd)
  try:
    process = subprocess.Popen(cmd, stdout=subprocess.PIPE, stderr=subprocess.STDOUT)
    if print_output:
      for line in iter(process.stdout.readline, b''):
        sys.stdout.write(line)
        sys.stdout.flush()
      process.stdout.close()
    process.wait()
  except:
    print ('calling {cmd} failed\n{trace}'.format(cmd=' '.join(cmd),trace=traceback.format_exc()))

@contextmanager
def cd(newdir):
  prevdir = os.getcwd()
  os.chdir(os.path.expanduser(newdir))
  try:
    yield
  finally:
    os.chdir(prevdir)
