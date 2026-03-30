import os
import re

base = r"c:\Users\aboub\Desktop\unikly\backend\matching-service\src\main\java\com\unikly\matchingservice"

imports_to_add = {
    "AiMatchingClient": "import com.unikly.matchingservice.adapter.out.provider.AiMatchingClient;",
    "ProcessedEventRepository": "import com.unikly.matchingservice.application.port.out.ProcessedEventRepository;",
    "FreelancerSkillCacheRepository": "import com.unikly.matchingservice.application.port.out.FreelancerSkillCacheRepository;"
}

for root, _, files in os.walk(base):
    for f in files:
        if not f.endswith(".java"): continue
        path = os.path.join(root, f)
        
        with open(path, "r", encoding="utf-8") as fn:
            c = fn.read()
            
        modified = False
        lines = c.split("\n")
        
        for class_name, import_stmt in imports_to_add.items():
            if class_name in c and f != class_name + ".java" and import_stmt not in c:
                for i, line in enumerate(lines):
                    if line.startswith("package "):
                        lines.insert(i + 1, import_stmt)
                        modified = True
                        break
        
        if modified:
            with open(path, "w", encoding="utf-8") as fn:
                fn.write("\n".join(lines))

print("Missing imports added")
