import json
import csv
import pandas as pd

#jaccard

users_friends={}
users=[]
user_matrix={}
user_neighbours={}
sum_similarities={}


#****************************************Jaccard Similarity**************************************************

#initier user 
infile = open(r"C:\Users\lenovo\Desktop\App_Rec\src\sample\datasets\User_Yelp_Final.json", "r")
u = infile.readlines()





# recuperer les listes d'amis de chaque user

for i in u:

  line = json.loads(i)

  user=line['user_id']

  friends=line['friends']

  user_neighbours[user]=[]

  users_friends[user]=friends



users=list(users_friends.keys())


#construire la matrice user_user

i=0
cpt=0
b=0
uslist=users
while users != [] :
  b +=1
  utilisateur=users[i]
  
  friends1=users_friends[utilisateur]
  f=len(friends1)
  friendset1=set(friends1)
  for us in range(1,len(users)):
    inter=0
    friends2=users_friends[users[us]]
    friendset2=set(friends2)
    
    inter=len(friendset1 & friendset2)

    if inter != 0:
    # calculer pour chaque 2 user la distance 
      union=f+len(friends2)-inter


    #enregistrer la distance dans une matrice 

      dist=1- (inter/union)

      user_matrix[ (utilisateur, users[us]) ]=dist

      user_neighbours[utilisateur].append(users[us])


      user_neighbours[users[us]].append(utilisateur)




      tab=sum_similarities.keys()
      if utilisateur in tab:
        sum_similarities[utilisateur]=sum_similarities[utilisateur]+(1-dist)
      else:
        sum_similarities[utilisateur]=1- dist
      if users[us] in tab:
        sum_similarities[users[us]]=sum_similarities[users[us]]+(1- dist)
      else:
        sum_similarities[users[us]]=1- dist
    cpt=cpt+1
    if cpt % 1000000 == 0:
      print(cpt)
      print(len(users))
  users.remove(utilisateur)
print(b)












user_item={}
user_avg={}
users=[]


#recupere la note moyenne que chaque utilisateur attribus

for line in u:
  
  l=json.loads(line)

  user=l['user_id']
  stars=l["average_stars"]
  user_item[user]={}
  users.append(user)
  user_avg[user]=stars
infile.close()



#****************************************Predictions************************************************


infile = open(r"C:\Users\lenovo\Desktop\App_Rec\src\sample\datasets\Review_Yelp_Friends_Final.json", "r")
r = infile.readlines()


#matrice user_item
for line in r:
  l=json.loads(line)
  user=l["user_id"]
  business=l["business_id"]
  stars=l["stars"]
  user_item[user][business]=stars

prediction=[]
reel=[]

key=user_matrix.keys()

filelist=[]

cpt=0

#pour chaque utilisateur on fait les predictions pour les items qu'il a évalué
for user in users:
  moyenne_principale=user_avg[user]
  items=user_item[user]

  if user in sum_similarities.keys():

    
    denominateur=sum_similarities[user]
    neighbours=user_neighbours[user]
    
    for item in items:
      #faire la prediction
      somme=0
      
      for us in neighbours:
        if item in user_item[us]:
          if (user , us ) in key:
            d=(user , us)
          
            note= user_item[us][item]
            avg=user_avg[us]
            sim= 1- user_matrix[d]
            somme = somme+ sim*(note-avg)

          else :
            d=(us , user)
            note= user_item[us][item]
            avg=user_avg[us]
            sim= 1- user_matrix[d]
            somme = somme+ sim*(note-avg)

      div= somme/denominateur
      pred=moyenne_principale+div

      if pred > 5: 
        pred = 5.0
      if pred < 0 :
        pred= 0.0

      filelist.append((user, item, pred))
      prediction.append(pred)
      reel.append(user_item[user][item])

      cpt+=1
      if cpt % 10000 == 0:
        print(cpt)
        print(div)


      
print(cpt)





#mae et rmse

from sklearn.metrics import mean_absolute_error
from sklearn.metrics import mean_squared_error


print("Mae:     " , mean_absolute_error(reel, prediction))
print("Rmse:    " , mean_squared_error(reel, prediction))




#write the results

o = open(r"C:\Users\lenovo\Desktop\App_Rec\src\sample\results\results_social_yelp.txt",'w')

for i in filelist:
  o.write(str(i[0])+" "+str(i[1])+" "+str(i[2])+"\n")
o.close()
