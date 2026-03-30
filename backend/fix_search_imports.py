import os

base = r"c:\Users\aboub\Desktop\unikly\backend\search-service\src\main\java\com\unikly\searchservice"

imports_to_add = {
    "UserProfileClient": "import com.unikly.searchservice.adapter.out.provider.UserProfileClient;",
    "ProcessedEventDocumentRepository": "import com.unikly.searchservice.application.port.out.ProcessedEventDocumentRepository;",
    "FreelancerDocumentRepository": "import com.unikly.searchservice.application.port.out.FreelancerDocumentRepository;",
    "JobDocumentRepository": "import com.unikly.searchservice.application.port.out.JobDocumentRepository;"
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
