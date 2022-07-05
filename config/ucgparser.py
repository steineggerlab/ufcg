import sys, json

def main():
    if len(sys.argv) != 3:
        print(f'USAGE: {sys.argv[0]} [UCG_IN] [PRO_IN]')
        sys.exit(1)

    pros = []
    with open(sys.argv[2]) as pin:
        while p := pin.readline()[:-1]:
            pros.append(p)

    with open(sys.argv[1]) as uin:
        uroot = json.load(uin)
        udata = uroot["data"]
        for pro in pros:
            print(f'{"1" if pro in udata else "0"}', end=' ')

    print('')

main()
