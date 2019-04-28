# Here some internals are described. 

The MultipleDFS feature requires the docusafe to divide the DFS into two parts.
0. The first part is the system_dfs. It contains for each user a userspace. This userspace contains a keystore, the list of the users
public keys and the DFS Credentials to the users DFS Connection. 

0. The seoond part is the users_dfs. It is used, as long as the user does not have an individual DFS Connection. In this case the
users data is stored below the users space in the users_dfs. But as soon, as the user registers its individual DFS Connection, all data is
transfered from this users_dfs/userspace to the DFS Connection of the user.

For this feature to run, two keystores are required. The first keystore is placed in the system_dfs/userspace. It only contains a private and a public key.
The public key is used to encrypt the users DFS Credentials. The private key is uese to decrypt the users DFS credentials. 
Further the system_dfs/userspace contains an inbox folder which contains files given from other users to this user. 

As all these documents are in the system_dfs and do not show any internal data of the user, their pathname is not encrypted.
The keystore is the bouncycastle UBER keystore and thus encrypted by itself. Only the DFS Credentials are encrypted as described above.

The sconed keystsore is created in the users DFS Connection, so by default in the users_dfs/userspace. This keystore contains a bunch of
public/private keypairs. The public keys are randomly choosen to encrypt the users data. Further the keystore contains exactly one
secrete key. This secret key is choosen to en- and decrypt the bucketpath of the users documents.


# create user

![New user sequence diagram](http://www.plantuml.com/plantuml/proxy?src=https://raw.githubusercontent.com/adorsys/docusafe/develop/.docs/mdfs-create-user.puml&fmt=png&vvv=1)

# write document

![New user sequence diagram](http://www.plantuml.com/plantuml/proxy?src=https://raw.githubusercontent.com/adorsys/docusafe/develop/.docs/mdfs-store-document.puml&fmt=png&vvv=1)

# read document

![New user sequence diagram](http://www.plantuml.com/plantuml/proxy?src=https://raw.githubusercontent.com/adorsys/docusafe/develop/.docs/mdfs-read-document.puml&fmt=png&vvv=1)

# destroy user

![New user sequence diagram](http://www.plantuml.com/plantuml/proxy?src=https://raw.githubusercontent.com/adorsys/docusafe/develop/.docs/mdfs-destroy-user.puml&fmt=png&vvv=1)
