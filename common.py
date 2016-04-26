import os, subprocess, traceback, sys
from contextlib import contextmanager

def run_cmd(cmd, print_output=False):
  output = ""
  if print_output:
    print ("Running %s" % ' '.join(cmd))
  try:
    process = subprocess.Popen(cmd, stdout=subprocess.PIPE, stderr=subprocess.STDOUT)
    if print_output:
      for line in iter(process.stdout.readline, b''):
        output = output + line
        sys.stdout.write(line)
        sys.stdout.flush()
      process.stdout.close()
    process.wait()
  except:
    print ('calling {cmd} failed\n{trace}'.format(cmd=' '.join(cmd),trace=traceback.format_exc()))
  return output

@contextmanager
def cd(newdir):
  prevdir = os.getcwd()
  os.chdir(os.path.expanduser(newdir))
  try:
    yield
  finally:
    os.chdir(prevdir)
