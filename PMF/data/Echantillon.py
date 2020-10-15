import pandas as pd

file=open("echantillon_yelp.txt",'w')

data = pd.read_csv('notes.txt', sep="  ", header=None)

sampled=data.sample(frac=0.3)

print(data.head())

a=0
for i in data:
    print(sampled[[0,1,2]].to_string(index=False,header=False))


file.write(sampled[[0,1,2]].to_string(index=False,header=False))