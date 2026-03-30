import os
import re

base_dir = r"c:\Users\aboub\Desktop\unikly\backend\job-service\src\main\java\com\unikly\jobservice"
adapter_dir = os.path.join(base_dir, r"adapter\out\persistence")
port_dir = os.path.join(base_dir, r"application\port\out")

os.makedirs(port_dir, exist_ok=True)

repos = ["JobRepository", "ProposalRepository", "ContractRepository", "InvitationRepository", "OutboxEventRepository"]

for repo in repos:
    # 1. Rename existing JpaRepository to SpringData{Repo}
    old_file = os.path.join(adapter_dir, f"{repo}.java")
    if not os.path.exists(old_file):
        continue
        
    with open(old_file, "r") as f:
        content = f.read()
        
    spring_data_content = content.replace(f"public interface {repo}", f"public interface SpringData{repo}")
    
    spring_data_file = os.path.join(adapter_dir, f"SpringData{repo}.java")
    with open(spring_data_file, "w") as f:
        f.write(spring_data_content)
        
    os.remove(old_file)
    print(f"Migrated {repo} to SpringData{repo}")

