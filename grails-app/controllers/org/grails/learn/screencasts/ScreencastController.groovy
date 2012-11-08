package org.grails.learn.screencasts

import org.grails.common.ApprovalStatus

class ScreencastController {
    def searchableService
    def cacheService
    static allowedMethods = [save: "POST", update: "POST", delete: "POST"]

    def index() {
        redirect(action: "list", params: params)
    }

    def edit(Long id) {
        def s = Screencast.get(id)
        if(s) {
            render view:"create", model:[screencastInstance: s]
        }
        else {
            render status:404
        }
    }

    def list() {
        def screencastInstanceList = Screencast.allQuery.list()
        [screencastInstanceList: screencastInstanceList]
    }

    def show() {
        def screencastInstance = Screencast.get(params.id)
        if (!screencastInstance) {
            redirect(action: "list")
        }
        else {
            [screencastInstance: screencastInstance]
        }
    }

    def create() {
        def screencastInstance = new Screencast()
        screencastInstance.properties = params
        return [screencastInstance: screencastInstance]
    }

    def save() {
        def screencast= params.id ? Screencast.get(params.id) : new Screencast()
        if(screencast == null) screencast = new Screencast()
        screencast.properties = params
        screencast.status = ApprovalStatus.PENDING
        screencast.submittedBy = request.user

        try {
            searchableService.stopMirroring()
            if (!screencast.hasErrors() && screencast.save()) {
                processTags screencast, params.tags
                def key = "screencast_${screencast.id}".toString()
                cacheService?.removeWikiText(key)
                cacheService?.removeShortenedWikiText(key)
  
                screencast.save flush: true
                flash.message = "Your submission was successful. We will let you know when it is approved."
                redirect(action: "list")
            }
            else {
                render(view: "create", model: [screencastInstance: screencast])
            }
        }
        catch (Exception ex) {
            ex.printStackTrace()
        }
        finally {
            searchableService.startMirroring()
        }
    }

    protected processTags(domainInstance, tagString) {
        def tags = tagString.split(/[,;]/)*.trim()
        domainInstance.tags = tags
    }
}
