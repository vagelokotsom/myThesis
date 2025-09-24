
# 🎓 **Student Container Management System**

## **Επισκόπηση Συστήματος**

Ένα ολοκληρωμένο σύστημα διαχείρισης εκπαιδευτικών containers που επιτρέπει σε καθηγητές να δημιουργούν SSH-enabled Ubuntu containers για φοιτητές, παρέχοντας ένα ασφαλές και ελεγχόμενο περιβάλλον εκμάθησης.

### 🚀 **Βασικά Χαρακτηριστικά**
- ✅ **Backend (Spring Boot + Kubernetes)**: Δημιουργία πραγματικών SSH-enabled containers
- ✅ **Frontend (React)**: Διαχείριση containers και εμφάνιση SSH οδηγιών
- ✅ **Kubernetes Integration**: Πλήρης ενσωμάτωση με Minikube για deployment
- ✅ **SSH Environment**: Οι φοιτητές μπορούν να συνδεθούν με SSH στα containers τους
- ✅ **Real-time Status**: Αυτόματη ανανέωση κατάστασης containers κάθε 30 δευτερόλεπτα
- ✅ **User Authentication**: Σύστημα εισόδου για καθηγητές και φοιτητές
- ✅ **RBAC Integration**: Kubernetes Role-Based Access Control για ασφάλεια

### 👥 **Ρόλοι Χρηστών**
- **Καθηγητές**: Δημιουργία containers για φοιτητές, παρακολούθηση όλων των containers
- **Φοιτητές**: Πρόσβαση στα δικά τους containers, SSH σύνδεση με αντιγραφή εντολών
- **Διαχειριστές**: Πλήρη διαχείριση χρηστών και containers

---

## 📁 **Δομή Project**
```
thesis/
├── thesis-backend-starter/           # Spring Boot Backend
│   ├── src/main/java/               # Java source code
│   ├── src/main/resources/          # Configuration files
│   ├── build.gradle                 # Dependencies & build config
│   ├── Dockerfile                   # Backend Docker image
│   └── Dockerfile.ssh               # SSH-enabled student container image
├── thesis_frontend_prototype/        # React Frontend
│   ├── src/                         # React components & pages
│   ├── public/                      # Static assets
│   ├── package.json                 # Dependencies
│   └── Dockerfile                   # Frontend Docker image
├── k8s/                             # Kubernetes Manifests
│   ├── rbac-setup.yaml             # RBAC configuration
│   ├── base/                       # Base Kubernetes resources
│   └── overlays/                   # Environment-specific overrides
└── README.md                        # Documentation
```

## 🚀 **Οδηγός Εγκατάστασης & Deployment**

### **📋 Προαπαιτούμενα**

- **Java 17+** (διαθέσιμο από [Oracle](https://www.oracle.com/java/technologies/downloads/) ή [OpenJDK](https://openjdk.org/))
- **Node.js 16+** και **npm** ([Download](https://nodejs.org/))
- **Docker Desktop** ([Download](https://www.docker.com/products/docker-desktop/))
- **Minikube** ([Installation Guide](https://minikube.sigs.k8s.io/docs/start/))
- **kubectl** ([Installation Guide](https://kubernetes.io/docs/tasks/tools/))
- **Gradle** (προαιρετικό - χρήση του `./gradlew`)

### **🔧 Βήμα 1: Εκκίνηση Minikube**

```bash
# Εκκίνηση Minikube με επαρκή resources
minikube start --memory=4096 --cpus=2 --driver=docker

# Επιβεβαίωση ότι το Minikube τρέχει
minikube status

# 🚨 ΚΡΙΣΙΜΟ: Σύνδεση με το Docker daemon του Minikube
eval $(minikube docker-env)

# Επιβεβαίωση σύνδεσης
docker ps
```

⚠️ **ΣΗΜΑΝΤΙΚΟ**: Το `eval $(minikube docker-env)` πρέπει να εκτελείται σε κάθε νέο terminal session!

### **🐳 Βήμα 2: Δημιουργία Docker Images**

```bash
# Μετάβαση στον φάκελο του project
cd thesis/thesis-backend-starter

# Δημιουργία Backend Image
docker build -t thesis-backend:latest .

# Δημιουργία SSH-enabled Student Container Image
docker build -f Dockerfile.ssh -t thesis-ssh-container:latest .

# 🚨 ΚΡΙΣΙΜΟ: Φόρτωση images στο Minikube
minikube image load thesis-backend:latest
minikube image load thesis-ssh-container:latest

# Επιβεβαίωση ότι τα images είναι διαθέσιμα
docker images | grep thesis
```

```bash
# Δημιουργία Frontend Image
cd ../thesis_frontend_prototype

# Εγκατάσταση dependencies (μόνο την πρώτη φορά)
npm install

# Δημιουργία production build
npm run build

# Δημιουργία Docker image
docker build -t thesis-frontend:v3 .

# Φόρτωση στο Minikube
minikube image load thesis-frontend:v3
```

### **☸️ Βήμα 3: Kubernetes Deployment**

```bash
# Μετάβαση στον φάκελο Kubernetes manifests
cd ../../k8s/overlays/minikube

# Δημιουργία RBAC resources για backend
kubectl apply -f - <<EOF
apiVersion: v1
kind: ServiceAccount
metadata:
  name: thesis-backend-sa
  namespace: default
---
apiVersion: rbac.authorization.k8s.io/v1
kind: Role
metadata:
  namespace: default
  name: pod-manager
rules:
- apiGroups: [""]
  resources: ["pods", "services"]
  verbs: ["get", "list", "watch", "create", "update", "patch", "delete"]
---
apiVersion: rbac.authorization.k8s.io/v1
kind: RoleBinding
metadata:
  name: thesis-backend-binding
  namespace: default
subjects:
- kind: ServiceAccount
  name: thesis-backend-sa
  namespace: default
roleRef:
  kind: Role
  name: pod-manager
  apiGroup: rbac.authorization.k8s.io
EOF

# Deploy όλα τα resources
kubectl apply -k .

# Επιβεβαίωση deployment
kubectl get pods -w
```

### **🌐 Βήμα 4: Πρόσβαση στην Εφαρμογή**

```bash
# Λήψη URLs για frontend και backend
minikube service frontend-service --url
minikube service backend-service --url

# Παράδειγμα output:
# Frontend: http://127.0.0.1:63354
# Backend: http://127.0.0.1:63355
```

**Ενημέρωση Frontend Environment:**
```bash
# Ενημέρωση του .env.production με το σωστό Backend URL
echo "REACT_APP_API_URL=http://127.0.0.1:63355" > thesis_frontend_prototype/.env.production

# Rebuild και redeploy frontend
docker build -t thesis-frontend:v3 thesis_frontend_prototype/
minikube image load thesis-frontend:v3
kubectl rollout restart deployment/thesis-frontend
```

### **✅ Βήμα 5: Επιβεβαίωση Λειτουργίας**

```bash
# Έλεγχος όλων των pods
kubectl get pods

# Έλεγχος services
kubectl get svc

# Test backend API
curl http://127.0.0.1:63355/api/auth/test
# Αναμενόμενη απάντηση: "Authentication is working!"
```

## 👥 **Χρήση της Εφαρμογής**

### **🔐 Πρόσβαση στο Σύστημα**

Το σύστημα υποστηρίζει τρεις ρόλους χρηστών με διαφορετικά δικαιώματα πρόσβασης. Οι χρήστες δημιουργούνται από τους διαχειριστές του συστήματος.

### **👨‍🏫 Ρόλοι & Δυνατότητες**

| Ρόλος | Δικαιώματα | Λειτουργίες |
|-------|------------|-------------|
| **Καθηγητής** | Container Management | Δημιουργία και διαχείριση containers για φοιτητές |
| **Φοιτητής** | Personal Containers | Πρόσβαση και SSH σε προσωπικά containers |
| **Διαχειριστής** | Full System Access | Πλήρη διαχείριση χρηστών και συστήματος |

### **📋 Ροή Εργασίας**

1. **Σύνδεση**: Εισαγωγή διαπιστευτηρίων που παρέχονται από τον διαχειριστή
2. **Καθηγητής**: 
   - Πρόσβαση στο "Container Management"
   - Δημιουργία containers για φοιτητές
   - Παρακολούθηση κατάστασης όλων των containers
3. **Φοιτητής**:
   - Πρόσβαση στο "My Containers" 
   - Προβολή διαθέσιμων containers
   - Λήψη SSH οδηγιών σύνδεσης

## � **SSH Σύνδεση στα Student Containers**

### **📱 Μέσω Web Interface (Προτεινόμενο)**

1. **Σύνδεση** στην εφαρμογή ως φοιτητής
2. **Πήγαινε** στο "My Containers"
3. **Πάτησε** "SSH Info" για το container σου
4. **Αντέγραψε** τις εντολές από το popup

### **💻 Χειροκίνητη Σύνδεση**

#### **Μέθοδος 1: Port Forwarding (Προτεινόμενο - Λειτουργεί Πάντα)**

```bash
# Βήμα 1: Εύρεση του SSH service name
kubectl get svc | grep ssh
# Παράδειγμα: container-student1-20250924195230-ssh

# Βήμα 2: Άνοιγμα Terminal 1 - Port Forward
kubectl port-forward service/container-student1-20250924195230-ssh 8023:22
# Θα δεις: "Forwarding from 127.0.0.1:8023 -> 22"
# ΚΡΑΤΑ ΑΥΤΟ ΤΟ TERMINAL ΑΝΟΙΧΤΟ!

# Βήμα 3: Άνοιγμα Terminal 2 - SSH Connection
ssh -o StrictHostKeyChecking=no -p 8023 root@127.0.0.1
# Password: student123
```

#### **Μέθοδος 2: Direct NodePort (Ενδέχεται να μη λειτουργεί σε macOS)**

```bash
# Λήψη Minikube IP
minikube ip
# π.χ. 192.168.49.2

# Εύρεση NodePort
kubectl get svc | grep ssh
# π.χ. 22:31456/TCP

# Σύνδεση
ssh -p 31456 root@192.168.49.2
# Password: student123
```

### **🔧 Αντιμετώπιση SSH Προβλημάτων**

#### **Πρόβλημα: "REMOTE HOST IDENTIFICATION HAS CHANGED"**
```bash
# Λύση: Αφαίρεση παλιού host key
ssh-keygen -R "[127.0.0.1]:8023"
# Μετά συνέχισε κανονικά με την SSH σύνδεση
```

#### **Πρόβλημα: "Connection Refused"**
```bash
# 1. Έλεγχος αν το container τρέχει
kubectl get pods | grep container-student

# 2. Έλεγχος logs
kubectl logs container-student1-20250924195230

# 3. Έλεγχος service
kubectl describe service container-student1-20250924195230-ssh
```

#### **Πρόβλημα: "Permission Denied"**
```bash
# Χρήση του σωστού username και password
ssh -o StrictHostKeyChecking=no -o PreferredAuthentications=password -p 8023 root@127.0.0.1
# Username: root
# Password: student123
```

## 🔍 Έλεγχος Κατάστασης

### **Backend Status**
```bash
# Έλεγχος backend μέσω minikube service URL
BACKEND_URL=$(minikube service backend-service --url)
curl $BACKEND_URL/api/auth/test

# Αναμενόμενη απάντηση: "Authentication is working!"
```

### **Kubernetes Status**
```bash
# Έλεγχος pods
kubectl get pods

# Έλεγχος services
kubectl get svc

# Έλεγχος όλων των resources
kubectl get all

# Minikube dashboard (γραφικό interface)
minikube dashboard
```

### **Database Status**
```bash
# Έλεγχος PostgreSQL database
kubectl logs deployment/postgres

# Έλεγχος database connectivity από backend
kubectl logs deployment/thesis-backend | grep -i database
```

## 🛠️ Προχωρημένες Εντολές

### **Rebuild & Redeploy Components**

#### **Backend**
```bash
cd thesis-backend-starter
./gradlew clean build -x test
docker build -t thesis-backend:latest .
minikube image load thesis-backend:latest
kubectl rollout restart deployment/thesis-backend
```

#### **Frontend**
```bash
cd thesis_frontend_prototype
npm run build
docker build -t thesis-frontend:v3 .
minikube image load thesis-frontend:v3
kubectl rollout restart deployment/thesis-frontend
```

#### **Student Container Image**
```bash
cd thesis-backend-starter
docker build -f Dockerfile.ssh -t thesis-ssh-container:latest .
minikube image load thesis-ssh-container:latest
```

### **🏃‍♂️ Γρήγορη Εκκίνηση (TL;DR)**

```bash
# 1. Εκκίνηση Minikube
minikube start --memory=4096 --cpus=2
eval $(minikube docker-env)

# 2. Build & Load Images
cd thesis/thesis-backend-starter
docker build -t thesis-backend:latest .
docker build -f Dockerfile.ssh -t thesis-ssh-container:latest .
cd ../thesis_frontend_prototype  
docker build -t thesis-frontend:v3 .

minikube image load thesis-backend:latest
minikube image load thesis-ssh-container:latest  
minikube image load thesis-frontend:v3

# 3. Deploy
cd ../k8s/overlays/minikube
kubectl apply -f ../../rbac-setup.yaml  # Create this file with RBAC config
kubectl apply -k .

# 4. Get URLs
minikube service frontend-service --url
minikube service backend-service --url

# 5. Update frontend API URL and redeploy
echo "REACT_APP_API_URL=http://127.0.0.1:[BACKEND_PORT]" > ../../thesis_frontend_prototype/.env.production
kubectl rollout restart deployment/thesis-frontend
```

### **🎯 Γρήγορη Αναφορά Εντολών**

```bash
# Γρήγορος έλεγχος κατάστασης
kubectl get pods,svc | grep -E "(thesis|postgres|container-)"

# Restart όλων των deployments
kubectl rollout restart deployment/thesis-backend deployment/thesis-frontend deployment/postgres

# Λήψη URLs για πρόσβαση
minikube service list

# Έλεγχος resource usage
kubectl top pods --sort-by=memory
```

## � **Οδηγός Αντιμετώπισης Προβλημάτων**

### **🔍 Διάγνωση Γενικών Προβλημάτων**

#### **Έλεγχος Κατάστασης Συστήματος**
```bash
# Έλεγχος Minikube
minikube status

# Έλεγχος όλων των pods
kubectl get pods -o wide

# Έλεγχος services
kubectl get svc

# Έλεγχος για errors στα events
kubectl get events --sort-by='.lastTimestamp'
```

### **🔧 Συνήθη Προβλήματα & Λύσεις**

#### **1. "ImagePullBackOff" στα Student Containers**
```bash
# Πρόβλημα: Το thesis-ssh-container:latest δεν βρίσκεται
# Λύση:
eval $(minikube docker-env)
cd thesis-backend-starter
docker build -f Dockerfile.ssh -t thesis-ssh-container:latest .
minikube image load thesis-ssh-container:latest

# Επανεκκίνηση του pod
kubectl delete pod [pod-name]
```

#### **2. "Frontend δε συνδέεται με Backend"**
```bash
# Πρόβλημα: Λάθος API URL στο frontend
# Λύση:
# 1. Λήψη σωστού backend URL
minikube service backend-service --url

# 2. Ενημέρωση environment variable
echo "REACT_APP_API_URL=http://127.0.0.1:YOUR_PORT" > thesis_frontend_prototype/.env.production

# 3. Rebuild frontend
cd thesis_frontend_prototype
docker build -t thesis-frontend:v3 .
minikube image load thesis-frontend:v3
kubectl rollout restart deployment/thesis-frontend
```

#### **3. "Backend δεν μπορεί να δημιουργήσει Pods"**
```bash
# Πρόβλημα: Μη σωστά RBAC permissions
# Λύση: Επαπλικαίρεση του service account
kubectl patch deployment thesis-backend -p '{"spec":{"template":{"spec":{"serviceAccountName":"thesis-backend-sa"}}}}'

# Έλεγχος permissions
kubectl auth can-i create pods --as=system:serviceaccount:default:thesis-backend-sa
```

#### **4. "Database Connection Errors"**
```bash
# Πρόβλημα: PostgreSQL δεν είναι διαθέσιμη
# Έλεγχος:
kubectl logs deployment/postgres

# Επανεκκίνηση database
kubectl rollout restart deployment/postgres
kubectl wait --for=condition=ready pod -l app=postgres --timeout=60s
```

#### **5. "Docker Images δεν βρίσκονται"**
```bash
# Πρόβλημα: Images δεν είναι διαθέσιμα στο Minikube
# Λύση:
# 1. Σύνδεση με Minikube Docker
eval $(minikube docker-env)

# 2. Έλεγχος διαθέσιμων images
docker images | grep thesis

# 3. Αν λείπουν, rebuild και load
docker build -t thesis-backend:latest thesis-backend-starter/
docker build -t thesis-frontend:v3 thesis_frontend_prototype/
docker build -f thesis-backend-starter/Dockerfile.ssh -t thesis-ssh-container:latest thesis-backend-starter/

minikube image load thesis-backend:latest
minikube image load thesis-frontend:v3
minikube image load thesis-ssh-container:latest
```

### **🔄 Πλήρης Reset του Συστήματος**

#### **Αν τίποτα δεν λειτουργεί:**
```bash
# 1. Διαγραφή όλων των resources
kubectl delete all --all
kubectl delete pvc --all
kubectl delete configmap --all
kubectl delete secret --all

# 2. Επανεκκίνηση Minikube
minikube stop
minikube delete
minikube start --memory=4096 --cpus=2 --driver=docker

# 3. Επανάληψη deployment από την αρχή
eval $(minikube docker-env)
# ... ακολούθησε τα βήματα deployment
```

### **📊 Χρήσιμες Εντολές Monitoring**

```bash
# Real-time pod monitoring  
kubectl get pods -w

# Resource usage
kubectl top pods
kubectl top nodes

# Detailed pod information
kubectl describe pod [pod-name]

# Logs από όλα τα containers
kubectl logs -f deployment/thesis-backend
kubectl logs -f deployment/thesis-frontend
kubectl logs -f deployment/postgres

# Minikube dashboard (γραφικό interface)
minikube dashboard
```

### **⚠️ Κοινές Παγίδες που Πρέπει να Αποφύγετε**

1. **Ξεχνάτε το `eval $(minikube docker-env)`** σε νέα terminal sessions
2. **Δεν κάνετε `minikube image load`** μετά από rebuild των images
3. **Χρησιμοποιείτε localhost URLs** αντί για minikube service URLs
4. **Δεν ελέγχετε τα logs** όταν κάτι δεν λειτουργεί
5. **Δεν επιβεβαιώνετε ότι τα pods είναι Ready** πριν τα χρησιμοποιήσετε

## 📖 API Documentation

### Authentication Endpoints
- `POST /api/auth/login` - Σύνδεση χρήστη
- `GET /api/auth/test` - Έλεγχος authentication

### Container Management
- `GET /api/containers/my-containers` - Containers του χρήστη
- `POST /api/containers/create-for-student` - Δημιουργία container (καθηγητές)
- `GET /api/containers/{id}/ssh-info` - SSH πληροφορίες
- `POST /api/containers/{id}/refresh-status` - Ανανέωση κατάστασης

### Image Templates
- `GET /api/images` - Διαθέσιμα templates

---

-- you can see a demo in here https://drive.google.com/file/d/1fdohHMIUZMHQjFXh8YItTLzdNB3PBgPy/view --- 
**Περιεχόμενο Demo:**
1. 🔐 Login ως καθηγητής και φοιτητής
2. 🐳 Δημιουργία νέου container για φοιτητή
3. ⚡ Real-time status updates
4. 🔧 SSH connection setup με port forwarding
5. 💻 Πλήρη πρόσβαση στο Ubuntu terminal
6. 📋 Copy-paste SSH commands από το UI
7. 🔄 Container lifecycle management

### 📸 Επιτυχημένη SSH Σύνδεση
Το screenshot δείχνει πλήρη πρόσβαση στο Ubuntu container:
```bash
ssh -p 8023 root@127.0.0.1
# Welcome to Ubuntu 20.04.6 LTS (GNU/Linux 6.10.14-linuxkit x86_64)
# root@container-student-20250707150626:~#
```
<img width="569" alt="Screenshot 2025-07-07 at 3 27 38 PM" src="https://github.com/user-attachments/assets/a668d918-ecff-4e2a-8f80-0648fa65f720" />



**Το screenshot αποδεικνύει:**
- ✅ Επιτυχή SSH authentication με password `student123`
- ✅ Πλήρη shell access με root privileges  
- ✅ Ubuntu 20.04.6 LTS environment
- ✅ Network connectivity μέσω port forwarding
- ✅ Πραγματικό terminal environment για εκπαιδευτικούς σκοπούς

---

## 📞 **Υποστήριξη & Συνεισφορά**

### **🐛 Αναφορά Προβλημάτων**
Αν αντιμετωπίζετε προβλήματα:
1. Ελέγξτε τον **Οδηγό Αντιμετώπισης Προβλημάτων** παραπάνω
2. Εκτελέστε `kubectl get events --sort-by='.lastTimestamp'` για errors
3. Ελέγξτε τα logs: `kubectl logs deployment/thesis-backend`

### **🔧 Τεχνική Υποστήριξη**
- **Repository**: [GitHub Repository](https://github.com/vagelokotsom/myThesis)
- **Issues**: [GitHub Issues](https://github.com/vagelokotsom/myThesis/issues)
- **Documentation**: Αυτό το README file

### **⭐ Acknowledgments**
- Spring Boot Team για το εξαιρετικό framework
- Kubernetes Community για την τεκμηρίωση
- React Team για το frontend framework
- Minikube Contributors για το local development environment

---

**🎓 Made with ❤️ for Educational Purposes**

*This project demonstrates modern container orchestration, microservices architecture, and educational technology integration using industry-standard tools and practices.*

