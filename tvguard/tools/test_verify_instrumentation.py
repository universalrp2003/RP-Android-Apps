import unittest

from verify_instrumentation import verify


CLASS = "com.universalrp.tvguard.TvRemoteFocusTest"


def event(name, code, class_name=CLASS):
    return (
        f"INSTRUMENTATION_STATUS: class={class_name}\n"
        f"INSTRUMENTATION_STATUS: test={name}\n"
        f"INSTRUMENTATION_STATUS_CODE: {code}\n"
    )


def successful(*tests):
    return "".join(event(name, 1) + event(name, 0) for name in tests) + "INSTRUMENTATION_CODE: -1\n"


class VerifyInstrumentationTest(unittest.TestCase):
    def test_accepts_exactly_required_passes(self):
        verify(successful("first", "second"), CLASS, {"first", "second"})

    def test_missing_or_duplicate_tests_fail(self):
        for text in (successful("first"), successful("first", "first", "second")):
            with self.subTest(text=text), self.assertRaises(ValueError):
                verify(text, CLASS, {"first", "second"})

    def test_failed_skipped_and_incomplete_tests_fail(self):
        for code in (-1, -2, -3, -4):
            text = event("first", 1) + event("first", code) + "INSTRUMENTATION_CODE: -1\n"
            with self.subTest(code=code), self.assertRaises(ValueError):
                verify(text, CLASS, {"first"})
        with self.assertRaises(ValueError):
            verify(event("first", 1) + "INSTRUMENTATION_CODE: -1\n", CLASS, {"first"})

    def test_wrong_class_or_no_successful_runner_result_fails(self):
        with self.assertRaises(ValueError):
            verify(event("first", 1, "other.Class") + event("first", 0, "other.Class") + "INSTRUMENTATION_CODE: -1\n", CLASS, {"first"})
        with self.assertRaises(ValueError):
            verify(successful("first").replace("INSTRUMENTATION_CODE: -1", "INSTRUMENTATION_CODE: 0"), CLASS, {"first"})

    def test_runner_crash_fails_even_with_test_events(self):
        with self.assertRaises(ValueError):
            verify(successful("first") + "Process crashed\n", CLASS, {"first"})


if __name__ == "__main__":
    unittest.main()
