kubectl config set-context --current --namespace=cim

kubectl scale deployment --replicas=1 --all -n cim

kubectl scale deployment --replicas=0 --all -n cim

kubectl get deployments -n cim

kubectl delete pod --all

kubectl rollout restart deployment dapr-sentry -n dapr-system
kubectl rollout restart deployment dapr-operator -n dapr-system

$serviceToken = openssl rand -base64 32
kubectl create secret generic clouply-service-token --from-literal=service-token=$serviceToken


# 1. Smaž starý deployment a související věci
kubectl delete deployment unit-web-gw-core -n cim --ignore-not-found
kubectl delete service unit-web-gw-core -n cim --ignore-not-found

# 2. Smaž namespace cim (volitelné, ale doporučené pro úplný reset)
# kubectl delete namespace cim


kubectl port-forward svc/unit-web-gw-core -n cim 8080:80