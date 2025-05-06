# c51cc

C51 Clojure Compiler

## Installation

Download from http://github.com/Sektel/c51cc_4

## Usage

lein compile [args]

## Options

file -- c files for compiler

## Examples

### Пример 1: Компиляция одного файла
```bash
lein compile example.c
```
Этот пример показывает, как скомпилировать один файл `example.c`.

### Пример 2: Компиляция нескольких файлов
```bash
lein compile file1.c file2.c
```
В этом примере компилируются два файла `file1.c` и `file2.c`.

### Пример 3: Использование дополнительных аргументов
```bash
lein compile --optimize example.c
```
Здесь мы используем флаг `--optimize` для оптимизации компиляции файла `example.c`.

### Пример 4: Компиляция с выводом в определённый каталог
```bash
lein compile --output-dir ./output example.c
```
Этот пример показывает, как указать каталог для вывода скомпилированного файла.

## License

MIT License

Copyright (c) 2025 Sehktel

Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
