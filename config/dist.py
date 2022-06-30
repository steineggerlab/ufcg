# dist.py [HMM_IN] [HMM_OUT]

import sys

def main():
    if len(sys.argv) != 3:
        print('USAGE : python dist.py [HMM_IN] [HMM_OUT]')
        sys.exit(0)

    fi, fo = open(sys.argv[1], 'r'), open(sys.argv[2], 'w')

    flag = True
    while line := fi.readline():
        if line[0] == '[':
            if 'dist' in line:
                flag = False
            else:
                flag = True
        if flag:
            print(line, end='', file=fo)

    fi.close()
    fo.close()

if __name__ == '__main__':
    main()
