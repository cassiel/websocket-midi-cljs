from flask import Flask, render_template
from flask_socketio import SocketIO, emit
from flask_cors import CORS, cross_origin

app = Flask(__name__)
socketio = SocketIO(app)
CORS(app)

@app.route('/')
def hello_world():
    return 'Hello, World!'

@socketio.on('my event')
def handle_my_custom_event(json):
    print('received json: ' + str(json))
    emit('to client', json, broadcast=True)

if __name__ == "__main__":
    socketio.run(app, debug=False)

# To deploy:
# gunicorn main:app
