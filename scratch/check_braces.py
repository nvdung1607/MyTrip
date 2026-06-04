import re

with open("c:/Users/Me di a se/Downloads/MyTrip/app/src/main/java/com/example/mytrip/ui/screens/today/TodayScreen.kt", "r", encoding="utf-8") as f:
    content = f.read()

lines = content.split('\n')
depth = 0
for idx, line in enumerate(lines, 1):
    # Find all braces on this line
    for char in line:
        if char == '{':
            depth += 1
            print(f"{idx:03d} (depth {depth}): OPEN in {line.strip()[:50]}")
        elif char == '}':
            depth -= 1
            print(f"{idx:03d} (depth {depth}): CLOSE in {line.strip()[:50]}")
