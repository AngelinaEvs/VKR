import os
from urllib import request
from zipfile import ZipFile
    import numpy as np
    import pandas as pd
    import matplotlib.pyplot as plt
    from PIL import Image
    from glob import glob

    from sklearn.model_selection import train_test_split
    from sklearn import metrics

    import cv2
    import gc
    import os

    import tensorflow as tf
    from tensorflow import keras
    from keras import layers

    import warnings

from flask import Flask, redirect, render_template

app = Flask(__name__)


def hello():
    warnings.filterwarnings('ignore')

    data_path = 'ROI.zip'

    with ZipFile(data_path, 'r') as zip:
        zip.extractall()


    path = 'ROI'
    classes = os.listdir(path)
    classes.remove('N')
    classes.remove('BG')
    print(classes)

    IMG_SIZE = 150
    SPLIT = 0.2
    EPOCHS = 20
    BATCH_SIZE = 64

    X = []
    Y = []

    for i, item in enumerate(classes):
        images = glob(f'{path}/{item}/*.jpg')

        for image in images:
            img = cv2.imread(image)

            X.append(cv2.resize(img, (IMG_SIZE, IMG_SIZE)))
            Y.append(i)

    X = np.asarray(X)
    one_hot_encoded_Y = pd.get_dummies(Y).values

    X_train, X_val, Y_train, Y_val = train_test_split(X, one_hot_encoded_Y, test_size=SPLIT, random_state=2022)

    model = keras.models.Sequential([
        layers.Conv2D(filters=64,
                      kernel_size=(5, 5),
                      activation='relu',
                      input_shape=(IMG_SIZE,
                                   IMG_SIZE,
                                   3),
                      padding='same'),
        layers.MaxPooling2D(3, 3),

        layers.Conv2D(filters=64,
                      kernel_size=(3, 3),
                      activation='relu',
                      padding='same'),
        layers.MaxPooling2D(2, 2),

        layers.Conv2D(filters=128,
                      kernel_size=(3, 3),
                      activation='relu',
                      padding='same'),
        layers.MaxPooling2D(2, 2),

        layers.Flatten(),
        layers.Dense(64, activation='relu'),
        layers.BatchNormalization(),
        layers.Dense(128, activation='relu'),
        layers.Dropout(0.5),
        layers.BatchNormalization(),
        layers.Dense(2, activation='softmax')
    ])

    print(model.summary())

    model.compile(
        optimizer='adam',
        loss='categorical_crossentropy',
        metrics=['accuracy']
    )

    from keras.callbacks import EarlyStopping, ReduceLROnPlateau

    es = EarlyStopping(patience=3,
                       monitor='val_accuracy',
                       restore_best_weights=True)

    lr = ReduceLROnPlateau(monitor='val_loss',
                           patience=2,
                           factor=0.5,
                           verbose=1)

    history = model.fit(X_train, Y_train,
                        validation_data=(X_val, Y_val),
                        batch_size=BATCH_SIZE,
                        epochs=EPOCHS,
                        verbose=1,
                        callbacks=[es, lr, myCallback()])

    history_df = pd.DataFrame(history.history)
    history_df.loc[:, ['loss', 'val_loss']].plot()
    history_df.loc[:, ['accuracy', 'val_accuracy']].plot()
    plt.show()

    Y_pred = model.predict(X_val)
    Y_val = np.argmax(Y_val, axis=1)


    return model

@app.route('/add_message', methods=['POST'])
def add_message(model):
    if 'image' not in request.files:
        return redirect('/add_message')
    file = request.files['image']
    file.save(file.filename)
    if file.filename == "":
        return redirect('/add_message')
    result = model.predict(file)
    for i in range(100):
        if os.path.isfile(file.filename):
            os.remove(file.filename)
    return result



if __name__ == '__main__':
    app.run()
    model = hello()
