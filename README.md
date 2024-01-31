# MTJ WORLDCUP 2022 AWS port

The same app as uploaded here https://github.com/pszemek90/mtj-worldcup-standings but using only serverless AWS resources.

Run locally:

- cdk synth
- sam local start-api -t ./cdk.out/MTJInfrastructureStack.template.json --docker-network cloud