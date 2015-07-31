# clj-honeybadger

Sends exceptions to honeybadger. This is fork of [ring-honeybadger](https://github.com/weavejester/ring-honeybadger) that doesn't assume ring.

This is useful when your service isn't a web server (i.e. - A worker process).

# Installation

Add to your dependencies in your `project.clj`:

```
[clj-honeybadger "0.1.0"]
```

# Usage

Use `send-exception!` to send an exception to honeybadger:

```
(require '[clj-honeybadger.core :as honeybadger])

(try
  ...
  (catch Exception e
    (honeybadger/send-exception! e {:api-key "honeybadger-api-key"
                                    :env "production"})))
```

