#!/usr/bin/env python2.7

import logging
import os
import sys
import platform
import pprint
import arg
import log
import jprint
import graphtools
import randoop

def print_tool(results,args):
    jprint.run_printer(results)

def graph_tool(results,args):
    graphtools.run(results,args)

def randoop_tool(results,args):
    randoop.run_randoop(results)


def log_header():
    logging.info('Running command %s', ' '.join(sys.argv))
    logging.info('Platform: %s', platform.platform())
    logging.info('PATH=%s', os.getenv('PATH'))
    logging.info('SHELL=%s', os.getenv('SHELL'))
    logging.info('PWD=%s', os.getenv('PWD'))

def main():
    args, cmd, imported_module = arg.parse_args()
    log.configure_logging(args.output_directory, args.incremental, args.log_to_stderr)

    log_header()

    results = imported_module.gen_instance(cmd).capture()
    logging.info('Results: %s', pprint.pformat(results))

    options = {'print' : print_tool,
               'randoop' : randoop_tool,
               'graphtool' : graph_tool,
    }

    if args.tool:
        options[args.tool](results,args)

if __name__ == '__main__':
    main()
