"""
A-USER-001: 首次安装信息采集 GUI (tkinter)
"""
import tkinter as tk
from tkinter import messagebox
import logging

logger = logging.getLogger('xdr-agent')

class UserInfoCollectorGUI:
    def __init__(self, on_submit):
        self.on_submit = on_submit
        self.root = tk.Tk()
        self.root.title("XDR Agent - 首次运行配置")
        self.root.geometry("400x480")
        self.root.resizable(False, False)
        
        # 居中显示
        screen_width = self.root.winfo_screenwidth()
        screen_height = self.root.winfo_screenheight()
        x = (screen_width / 2) - (400 / 2)
        y = (screen_height / 2) - (480 / 2)
        self.root.geometry(f'400x480+{int(x)}+{int(y)}')

        self._create_widgets()

    def _create_widgets(self):
        tk.Label(self.root, text="请完善用户信息以完成资产注册", font=("微软雅黑", 12, "bold")).pack(pady=10)
        
        fields = [
            ("一级单位(必填):", "unitLevel1"),
            ("二级单位(必填):", "unitLevel2"),
            ("三级单位(选填):", "unitLevel3"),
            ("四级单位(选填):", "unitLevel4"),
            ("所属部门(选填):", "department"),
            ("用户岗位(选填):", "post"),
            ("使用人(必填):", "username"),
            ("手机号(必填):", "phone"),
            ("常用邮箱(选填):", "email")
        ]
        
        self.entries = {}
        for label_text, key in fields:
            frame = tk.Frame(self.root)
            frame.pack(fill='x', padx=20, pady=5)
            tk.Label(frame, text=label_text, width=12, anchor='e').pack(side='left')
            entry = tk.Entry(frame)
            entry.pack(side='left', fill='x', expand=True, padx=5)
            self.entries[key] = entry

        tk.Button(self.root, text="确认并上报", command=self._submit, bg="#409EFF", fg="white", width=15).pack(pady=20)

    def _submit(self):
        data = {k: v.get().strip() for k, v in self.entries.items()}
        
        # 简单校验
        if not data['unitLevel1'] or not data['unitLevel2'] or not data['username'] or not data['phone']:
            messagebox.showwarning("输入项错误", "一级单位、二级单位、姓名和手机号为必填项！")
            return
            
        # 自动补齐未填写的选填项
        for key in ['unitLevel3', 'unitLevel4', 'department', 'post', 'email']:
            if not data[key]:
                data[key] = "-"
            
        self.on_submit(data)
        messagebox.showinfo("成功", "资产用户信息已采集并上报")
        self.root.destroy()

    def run(self):
        self.root.mainloop()

if __name__ == "__main__":
    # 测试代码
    setup = UserInfoCollectorGUI(lambda d: print(f"Collected: {d}"))
    setup.run()
