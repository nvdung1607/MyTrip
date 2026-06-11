import os

def process_file(filepath):
    with open(filepath, 'r', encoding='utf-8') as f:
        content = f.read()

    new_content = content.replace('Icons.Filled', 'Icons.Rounded')
    new_content = new_content.replace('Icons.Default', 'Icons.Rounded')
    new_content = new_content.replace('icons.filled', 'icons.rounded')
    new_content = new_content.replace('icons.default', 'icons.rounded')

    if content != new_content:
        with open(filepath, 'w', encoding='utf-8') as f:
            f.write(new_content)
        print(f"Refactored icons in: {filepath}")

for root, _, files in os.walk('app/src/main/java'):
    for file in files:
        if file.endswith('.kt'):
            process_file(os.path.join(root, file))
