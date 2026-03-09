import urllib.request
import json

res = urllib.request.urlopen('http://localhost:8082/api/v1/assets').read().decode()
data = json.loads(res)['data']
if not data['records']:
    print("No agents found")
    exit()

aid = data['records'][0]['agentId']
print("Agent ID:", aid)

res2 = urllib.request.urlopen(f'http://localhost:8082/api/v1/assets/{aid}/details').read().decode()
details = json.loads(res2)['data']

print("Processes:", len(details.get('processes', [])))
if details.get('processes'):
    print("PID:", details['processes'][0].get('pid'))
    print("createTime:", details['processes'][0].get('createTime'))
    print("cpuPercent:", details['processes'][0].get('cpuPercent'))

print("\nPorts:", len(details.get('ports', [])))
if details.get('ports'):
    print(details['ports'][0])
