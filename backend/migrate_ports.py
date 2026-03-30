import os
import re

base_dir = r"c:\Users\aboub\Desktop\unikly\backend\job-service\src\main\java\com\unikly\jobservice"
adapter_dir = os.path.join(base_dir, r"adapter\out\persistence")
port_dir = os.path.join(base_dir, r"application\port\out")
service_dir = os.path.join(base_dir, r"application\service")

os.makedirs(port_dir, exist_ok=True)

repos = ["JobRepository", "ProposalRepository", "ContractRepository", "InvitationRepository", "OutboxEventRepository"]

for repo in repos:
    port_content = f"""package com.unikly.jobservice.application.port.out;

import java.util.Optional;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import com.unikly.jobservice.domain.model.*;
import com.unikly.common.outbox.OutboxEvent;

public interface {repo} {{
    // Implement standard and custom methods matching SpringData
}}
"""
    
    with open(os.path.join(port_dir, f"{repo}.java"), 'w') as f:
        f.write(port_content)
        
    adapter_content = f"""package com.unikly.jobservice.adapter.out.persistence;

import com.unikly.jobservice.application.port.out.{repo};
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import java.util.Optional;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import com.unikly.jobservice.domain.model.*;
import com.unikly.common.outbox.OutboxEvent;

@Component
@RequiredArgsConstructor
public class {repo}Adapter implements {repo} {{
    private final SpringData{repo} repository;
}}
"""
    with open(os.path.join(adapter_dir, f"{repo}Adapter.java"), 'w') as f:
        f.write(adapter_content)
        
    print(f"Created Port and Adapter for {repo}")
    
# Now rewrite all usages of com.unikly.jobservice.adapter.out.persistence.{Repo} to com.unikly.jobservice.application.port.out.{Repo} in services
for root, _, files in os.walk(service_dir):
    for fn in files:
        if not fn.endswith(".java"): continue
        path = os.path.join(root, fn)
        with open(path, 'r') as f:
            c = f.read()
            
        c = re.sub(r'import com\.unikly\.jobservice\.adapter\.out\.persistence\.(\w+Repository);', r'import com.unikly.jobservice.application.port.out.\1;', c)
        
        with open(path, 'w') as f:
            f.write(c)
