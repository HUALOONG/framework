import sys, xml.etree.ElementTree as ET

p = sys.argv[1]
t = ET.parse(p)
r = t.getroot()
tot_m = tot_c = 0
out = []
for pkg in r.findall('package'):
    for sf in pkg.findall('sourcefile'):
        name = sf.get('name')
        missed = []   # fully missed lines (ci == 0)
        partial = []  # partially covered lines
        for line in sf.findall('line'):
            ci = int(line.get('ci'))
            mi = int(line.get('mi'))
            if mi > 0:
                if ci == 0:
                    missed.append(line.get('nr'))
                else:
                    partial.append(line.get('nr'))
            tot_c += 1 if ci > 0 else 0
            tot_m += 1 if (ci == 0 and mi > 0) else 0
        if missed or partial:
            out.append((pkg.get('name'), name, missed, partial))
out.sort(key=lambda x: -len(x[2]))
for pkg, name, missed, partial in out:
    print(f"{pkg}/{name}: FULL_MISSED={len(missed)} [{','.join(missed)}]  PARTIAL={len(partial)} [{','.join(partial)}]")
print(f"LINE_TOTAL {tot_c}/{tot_c + tot_m} = {tot_c / (tot_c + tot_m) * 100:.2f}% missed={tot_m}")
