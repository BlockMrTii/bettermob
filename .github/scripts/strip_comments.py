import re
import sys


def strip(source):
    out = []
    i = 0
    n = len(source)
    while i < n:
        two = source[i:i + 2]
        if source.startswith('"""', i):
            end = i + 3
            while end < n and not source.startswith('"""', end):
                end += 2 if source[end] == '\\' else 1
            out.append(source[i:end + 3])
            i = end + 3
        elif source[i] in '"\'':
            quote = source[i]
            end = i + 1
            while end < n and source[end] != quote and source[end] != '\n':
                end += 2 if source[end] == '\\' else 1
            out.append(source[i:end + 1])
            i = end + 1
        elif two == '//':
            while i < n and source[i] != '\n':
                i += 1
        elif two == '/*':
            end = source.find('*/', i + 2)
            end = n if end < 0 else end + 2
            out.append(' ' if '\n' not in source[i:end] else '\n' * source.count('\n', i, end))
            i = end
        else:
            out.append(source[i])
            i += 1
    return ''.join(out)


def normalise(source):
    lines = [line.rstrip() for line in source.split('\n')]
    result = []
    for line in lines:
        if line == '' and result and result[-1] == '':
            continue
        result.append(line)
    text = '\n'.join(result).strip('\n')
    text = re.sub(r'\{\n\n', '{\n', text)
    text = re.sub(r'\n\n(\s*\})', r'\n\1', text)
    return text + '\n'


def process(path):
    with open(path, encoding='utf-8', newline='') as handle:
        original = handle.read()
    text = original.replace('\r\n', '\n')
    cleaned = strip(text)
    if cleaned == text:
        return False
    with open(path, 'w', encoding='utf-8', newline='') as handle:
        handle.write(normalise(cleaned))
    return True


if __name__ == '__main__':
    changed = [path for path in sys.argv[1:] if process(path)]
    print('\n'.join(changed))
