IMAGE = snice
REGISTRY = registry.snice.io
TAG_LATEST = $(REGISTRY)/$(IMAGE):latest

compile:
	mvn clean install

build:
	docker build -t ${TAG_LATEST} .

run:
	docker run -p 8000:8080 ${TAG_LATEST}

login:
	docker login ${REGISTRY}

push: 
	docker push ${TAG_LATEST}
