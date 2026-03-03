import re
import yaml

class SignatureMatcher:
    """
    威胁特征匹配引擎
    """
    def __init__(self, rules_path):
        self.rules = []
        try:
            with open(rules_path, "r", encoding="utf-8") as f:
                data = yaml.safe_load(f)
                self.rules = data.get("rules", [])
        except Exception:
            pass

    def match(self, text):
        for rule in self.rules:
            pattern = rule.get("pattern")
            if pattern and re.search(pattern, text, re.I):
                return rule
        return None
