import unittest

from strip_comments import normalise, strip


class StripCommentsTest(unittest.TestCase):
    def clean(self, source):
        return normalise(strip(source))

    def test_removes_line_comments(self):
        self.assertEqual('int a = 1;\n', self.clean('int a = 1; // one\n'))

    def test_removes_whole_line_and_block_comments(self):
        source = 'class A {\n    // note\n    /** doc\n     * more */\n    int a;\n}\n'
        self.assertEqual('class A {\n    int a;\n}\n', self.clean(source))

    def test_keeps_comment_markers_inside_strings_and_chars(self):
        source = 'String s = "http://x /* y */";\nchar c = \'"\';\n'
        self.assertEqual(source, self.clean(source))

    def test_keeps_text_blocks(self):
        source = 'String s = """\n    // keep\n    """;\n'
        self.assertEqual(source, self.clean(source))

    def test_inline_block_comment_does_not_join_tokens(self):
        self.assertEqual(['int', 'a;'], self.clean('int/* x */a;\n').split())

    def test_collapses_blank_lines_left_behind(self):
        self.assertEqual('int a;\n\nint b;\n', self.clean('int a;\n\n// x\n\nint b;\n'))


if __name__ == '__main__':
    unittest.main()
