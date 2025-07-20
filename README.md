
### Τι έχει υλοποιηθεί:
- ✅ **Backend (Spring Boot + Kubernetes)**: Δημιουργία πραγματικών SSH-enabled containers
- ✅ **Frontend (React)**: Διαχείριση containers και εμφάνιση SSH οδηγιών
- ✅ **Kubernetes Integration**: Πλήρης ενσωμάτωση με Minikube για deployment
- ✅ **SSH Environment**: Οι φοιτητές μπορούν να συνδεθούν με SSH στα containers τους
- ✅ **Real-time Status**: Αυτόματη ανανέωση κατάστασης containers κάθε 30 δευτερόλεπτα
- ✅ **User Authentication**: Σύστημα εισόδου για καθηγητές και φοιτητές

### Λειτουργίες:
- **Καθηγητές**: Δημιουργία containers για φοιτητές, παρακολούθηση όλων των containers
- **Φοιτητές**: Πρόσβαση στα δικά τους containers, SSH σύνδεση με αντιγραφή εντολών
- **Διαχειριστές**: Πλήρη διαχείριση χρηστών και containers

### Open issues (Τα χρειαζόμαστε;)
- Actual delete pods if needed (in progress)
- Admin role 
- New template creation + new students, υπάρχουν προκαθορισμένοι χρήστες στα πλαίσια του demo.

---

## 📁 Δομή Project
```
thesis/
├── thesis-backend-starter/    # Spring Boot Backend
│   ├── src/main/java/        # Java source code
│   ├── src/main/resources/   # Configuration files
│   ├── build.gradle          # Dependencies & build config
│   ├── Dockerfile.ssh        # SSH-enabled Docker image
│   └── k8s-deployment.yaml   # Kubernetes deployment
├── thesis_frontend_prototype/ # React Frontend
│   ├── src/                  # React components & pages
│   ├── public/               # Static assets
│   ├── package.json          # Dependencies
│   └── build/                # Production build
├── mariadb-*.yaml            # Database configurations
├── *.sql                     # Database initialization scripts
└── README.md                 # Αυτό το αρχείο
```

## 🚀 Γρήγορη Εκκίνηση

### 1. Προαπαιτούμενα

- Java 17+
- Node.js 16+ και npm
- Docker
- [Minikube](https://minikube.sigs.k8s.io/docs/start/)
- [kubectl](https://kubernetes.io/docs/tasks/tools/)
- Gradle (ή χρήση του `./gradlew`)

### 2. Εκκίνηση Minikube

```bash
# Εκκίνηση Minikube
minikube start

# Χρήση του Docker του Minikube
eval $(minikube docker-env)
it is important to run this command in order for docker to communicate with minikube's Docked daemon.
```

### 3. Δημιουργία SSH-enabled Docker Image

```bash
# Μετάβαση στον φάκελο backend
cd thesis-backend-starter

# Δημιουργία SSH-enabled image
docker build -f Dockerfile.ssh -t thesis-ssh-container:latest .

When we run:

docker build -f Dockerfile.ssh -t thesis-ssh-container:latest .
We're building the image on your local Docker daemon, but:

Kubernetes running in Minikube has its own internal image registry (its own Docker/Containerd daemon).

So your image isn't visible to Kubernetes unless you do one of the following.

Load the image into Minikube
If you're using Minikube, run:

bash
Copy
Edit
minikube image load thesis-ssh-container:latest
This loads your locally built image into Minikube's container runtime, making it available even if imagePullPolicy: Never.
```

### 4. Εκκίνηση Backend

```bash
# Από τον φάκελο thesis-backend-starter
./gradlew bootRun
Or if you are using intelij IDEA you can run the main application.
```

Το backend θα εκκινήσει στο http://localhost:8080

### 5. Εκκίνηση Frontend

```bash
# Σε νέο terminal, μετάβαση στον φάκελο frontend
cd thesis_frontend_prototype

# Εγκατάσταση dependencies (μόνο την πρώτη φορά)
npm install

# Εκκίνηση development server
npm start
```

Το frontend θα εκκινήσει στο http://localhost:3000

## 👥 Χρήση της Εφαρμογής

### Προκαθορισμένοι Χρήστες:

| Όνομα Χρήστη | Κωδικός | Ρόλος | Περιγραφή |
|---------------|---------|-------|-----------|
| `teacher` | `TeachSecure2024!` | Καθηγητής | Δημιουργία containers για φοιτητές |
| `student` | `StudyHard2024#` | Φοιτητής | Πρόσβαση σε προσωπικά containers |
| `admin` | `AdminPower2024$` | Διαχειριστής | Πλήρη διαχείριση συστήματος |

### Ροή Εργασίας:

1. **Σύνδεση**: Χρήση των παραπάνω διαπιστευτηρίων
2. **Καθηγητής**: 
   - Πήγαινε στο "Container Management"
   - Δημιούργησε container για φοιτητή
   - Παρακολούθησε την κατάσταση όλων των containers
3. **Φοιτητής**:
   - Πήγαινε στο "My Containers" 
   - Δες τα διαθέσιμα containers
   - Πάτησε "SSH Info" για οδηγίες σύνδεσης

## 🔧 SSH Σύνδεση

Όταν ένα container είναι έτοιμο, οι φοιτητές θα δουν:

### Μέθοδος 1: Άμεση Σύνδεση (ενδέχεται να μη λειτουργεί σε macOS)
```bash
ssh -p [NodePort] root@[MinikubeIP]
```

### Μέθοδος 2: Port Forwarding (Προτεινόμενο για macOS/Minikube)
```bash
# Βήμα 1: Άνοιγμα terminal
kubectl port-forward service/[service-name] 8023:22

# Βήμα 2: Σε νέο terminal
ssh -p 8023 root@127.0.0.1

# Κωδικός: student123
```

### Γιατί Διαφορετικές Πόρτες;

- **NodePort (π.χ. 31945)**: Τυχαία πόρτα που δίνει το Kubernetes (30000-32767)
- **Port Forward (8023)**: Τοπική πόρτα που επιλέγουμε για ευκολία

Η εφαρμογή παρέχει αυτόματα αντιγραφή όλων των εντολών με κουμπιά "Copy"!

## 🔍 Έλεγχος Κατάστασης

### Backend Status
```bash
# Έλεγχος αν το backend τρέχει
curl http://localhost:8080/api/auth/test

# Αναμενόμενη απάντηση: "Authentication is working!"
```

### Kubernetes Status
```bash
# Έλεγχος pods
kubectl get pods or run in terminal minikube dashboard to see pods.

# Έλεγχος services
kubectl get svc

# Έλεγχος όλων των resources
kubectl get all
```

### Database Status
```bash
# Το backend χρησιμοποιεί H2 in-memory database
# Δεδομένα διαθέσιμα στο: http://localhost:8080/h2-console
# JDBC URL: jdbc:h2:mem:testdb
```

## 🛠️ Προχωρημένες Εντολές

### Rebuild Backend
```bash
cd thesis-backend-starter
./gradlew clean build -x test
./gradlew bootRun
```

### Rebuild Frontend
```bash
cd thesis_frontend_prototype
npm run build
npm start
```

### Reset Kubernetes Environment
```bash
# Διαγραφή όλων των pods και services
kubectl delete pods --all
kubectl delete services --all --selector=app!=kubernetes

# Επανεκκίνηση Minikube
minikube stop
minikube start
eval $(minikube docker-env)
```

### Debug SSH Connections
```bash
# Έλεγχος αν το SSH service τρέχει
kubectl get svc | grep ssh

# Έλεγχος logs από SSH container
kubectl logs [pod-name]

# Test SSH connectivity
ssh -p [port] -o ConnectTimeout=5 root@[host]
```

## 📋 Troubleshooting

### Συνήθη Προβλήματα:

1. **"Backend not responding"**
   ```bash
   # Έλεγχος αν τρέχει
   lsof -i :8080
   # Αν όχι, εκκίνηση ξανά
   cd thesis-backend-starter && ./gradlew bootRun
   ```

2. **"SSH connection refused"**
   ```bash
   # Χρήση port forwarding αντί για άμεση σύνδεση
   kubectl port-forward service/[service-name] 8023:22
   ssh -p 8023 root@127.0.0.1
   ```

3. **"Container stuck in Pending"**
   ```bash
   # Έλεγχος events
   kubectl describe pod [pod-name]
   # Πιθανά θέματα με resources ή images
   ```

4. **"Minikube not accessible"**
   ```bash
   # Επανεκκίνηση Minikube
   minikube stop && minikube start
   eval $(minikube docker-env)
   ```

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



**Αποδεικνύει:**
- Επιτυχή SSH authentication
- Πλήρη shell access
- Ubuntu 20.04.6 LTS environment
- Root privileges για educational purposes
  Network connectivity μέσω port forwarding

