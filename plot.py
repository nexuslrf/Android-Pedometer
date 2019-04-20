import matplotlib.pyplot as plt 
import numpy as np

acc_pre = np.array([eval(line.split()[-2]) for line in open("acc_preprocess_0.10.txt").readlines()])[5000:25000]
step = np.array([eval(line.split()[-1]) for line in open("acc_preprocess_0.10.txt").readlines()])[5000:25000]
acc_raw = np.array([eval(line.split()[-2]) for line in open("acc_raw_0.10.txt").readlines()])[5000:25000]
acc_flt = acc_raw - acc_pre
mod = []
prev = step[0]
for x in step:
    mod.append(x-prev)
    prev = x

acc_ff = []
tmp = acc_pre[0]
for i in acc_pre.tolist():
    tmp = i * 0.05 + tmp * 0.95
    acc_ff.append(tmp)

plt.figure(figsize=(20,12))
plt.plot(acc_raw, label="Raw Data")
plt.xlabel(r"$t$", fontsize=20)
plt.ylabel(r"$a/m\cdot s^{-2}$", fontsize=20)
# plt.figure(figsize=(20,12))
plt.plot(acc_flt, label="Low Pass")
plt.legend(fontsize=20)
plt.figure(figsize=(20,12))
plt.plot(acc_pre, label="After Process")
plt.xlabel(r"$t$", fontsize=20)
plt.ylabel(r"$a/m\cdot s^{-2}$", fontsize=20)
plt.plot(acc_ff, label='ff')
plt.plot(mod)
plt.legend(fontsize=20)
plt.show()