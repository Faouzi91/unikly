import os
import re

base_dir = r"c:\Users\aboub\Desktop\unikly\backend\job-service\src\main\java\com\unikly\jobservice"
adapter_dir = os.path.join(base_dir, r"adapter\out\persistence")
port_dir = os.path.join(base_dir, r"application\port\out")

repos = ["JobRepository", "ProposalRepository", "ContractRepository", "InvitationRepository", "OutboxEventRepository"]

for repo in repos:
    # 1. Delete the generated adapter and port
    adapter_file = os.path.join(adapter_dir, f"{repo}Adapter.java")
    port_file = os.path.join(port_dir, f"{repo}.java")
    if os.path.exists(adapter_file): os.remove(adapter_file)
    if os.path.exists(port_file): os.remove(port_file)
    
    # 2. Rename SpringDataRepo to Repo and move to port_dir
    spring_data_file = os.path.join(adapter_dir, f"SpringData{repo}.java")
    if not os.path.exists(spring_data_file): continue
    
    with open(spring_data_file, "r") as f:
        content = f.read()
        
    content = content.replace(f"public interface SpringData{repo}", f"public interface {repo}")
    content = content.replace("package com.unikly.jobservice.adapter.out.persistence;", "package com.unikly.jobservice.application.port.out;")
    
    with open(port_file, "w") as f:
        f.write(content)
        
    os.remove(spring_data_file)
    print(f"Moved {repo} into port")
